import json
from locust import task
from locust.contrib.fasthttp import FastHttpUser

# --- CONFIGURATION ---
ACTION_ID = 13
DESTINATION_URL = "/items/test-item-123"
HOST = "http://greenlight-core-api-alb-104707260.ap-northeast-2.elb.amazonaws.com"
# --- END CONFIGURATION ---

class QueueUser(FastHttpUser):
    # think-time 제거
    wait_time = lambda self: 0  # 완전 0으로 설정 [web:37]

    # 타깃 호스트 설정
    host = HOST

    # 느린 커넥션이 동시성 슬롯을 붙잡지 않도록 타임아웃 단축
    connection_timeout = 5.0     # TCP connect timeout [web:41]
    network_timeout = 5.0        # read/write timeout [web:41]

    # 공용 바디/헤더 준비(직렬화 비용 최소화)
    payload = {"actionId": ACTION_ID, "destinationUrl": DESTINATION_URL}
    body = json.dumps(payload)
    json_headers = {
        # 헤더 기본값 명시로 재시도/대소문자 혼선 방지
        "Content-Type": "application/json",
        "Accept": "application/json",
        # 압축 해제해 CPU/디코딩 비용 축소(환경에 따라 성능 비교 권장)
        "Accept-Encoding": "identity",
        # keep-alive 명시(HTTP/1.1 기본이지만 의도 명확화)
        "Connection": "keep-alive",
    }

    @task
    def single_user_flow(self):
        # 1) check-or-enter
        with self.client.post(
            "/api/v1/queue/check-or-enter",
            data=self.body,
            headers=self.json_headers,
            name="/check-or-enter",
            catch_response=True,
        ) as r1:
            if r1.status_code != 200:
                r1.failure(f"check-or-enter {r1.status_code}")
                self.stop(True); return
            try:
                data = r1.json()
            except Exception:
                r1.failure("bad json from check-or-enter")
                self.stop(True); return

        jwt = data.get("jwtToken")
        status = data.get("waitStatus")

        # 2) READY -> verify
        if status == "READY" and jwt:
            with self.client.post(
                "/api/v1/customer/verify",
                headers={"X-GREENLIGHT-TOKEN": jwt},
                name="/verify",
                catch_response=True,
            ) as r2:
                if r2.status_code == 200:
                    r2.success()
                else:
                    r2.failure(f"verify {r2.status_code}")

        # 단일 플로우 후 종료(요구사항 유지)
        self.stop(True)
