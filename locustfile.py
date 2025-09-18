import json
import time
from locust import HttpUser, task, between, events
import sseclient

# --- CONFIGURATION ---
ACTION_ID = 13
DESTINATION_URL = "/items/test-item-123"
SSE_TIMEOUT = 180
# --- END CONFIGURATION ---

class QueueUser(HttpUser):
    wait_time = between(1, 3)  # 각 태스크 실행 후 1~3초 대기
    host = "http://greenlight-core-api-alb-104707260.ap-northeast-2.elb.amazonaws.com"
    
    def on_start(self):
        """사용자가 시뮬레이션 시작 시 실행되는 메서드"""
        self.jwt_token = None
        self.customer_id = None
        self.wait_status = None

        payload = {"actionId": ACTION_ID, "destinationUrl": DESTINATION_URL}
        
        with self.client.post("/api/v1/queue/check-or-enter", json=payload, name="/check-or-enter", catch_response=True) as response:
            if response.status_code == 200:
                data = response.json()
                self.jwt_token = data.get("jwtToken")
                self.customer_id = data.get("customerId")
                self.wait_status = data.get("waitStatus")
            else:
                response.failure(f"Failed to check-or-enter, status code: {response.status_code}")
                return

        # 대기 상태이면 SSE 대기 로직 실행
        if self.wait_status != "READY":
            self.wait_for_ready_status()

    def wait_for_ready_status(self):
        """SSE 연결을 통해 READY 상태가 될 때까지 대기"""
        if not self.customer_id:
            return

        sse_url = f"{self.host}/waiting/sse?actionId={ACTION_ID}&customerId={self.customer_id}"
        
        try:
            # Locust의 HTTP 클라이언트는 SSE를 직접 지원하지 않으므로, 별도 클라이언트 사용
            response = self.client.get(f"/waiting/sse?actionId={ACTION_ID}&customerId={self.customer_id}", name="/sse", stream=True, timeout=SSE_TIMEOUT)
            sse_client = sseclient.SSEClient(response)

            start_time = time.time()
            for event in sse_client.events():
                if time.time() - start_time > SSE_TIMEOUT:
                    events.request.fire(
                        request_type="SSE", name="timeout", response_time=SSE_TIMEOUT * 1000,
                        response_length=0, exception=TimeoutError("SSE connection timed out")
                    )
                    return

                queue_info = json.loads(event.data)
                current_status = queue_info.get("waitStatus")
                if current_status == "READY":
                    self.wait_status = "READY"
                    # 성공 이벤트 발생
                    events.request.fire(
                        request_type="SSE", name="ready", response_time=(time.time() - start_time) * 1000,
                        response_length=len(event.data), exception=None
                    )
                    return
        except Exception as e:
            events.request.fire(
                request_type="SSE", name="error", response_time=0,
                response_length=0, exception=e
            )


    @task
    def verify_entry(self):
        """READY 상태가 된 후 최종 입장을 검증하는 태스크"""
        if self.wait_status != "READY" or not self.jwt_token:
            return

        headers = {"X-GREENLIGHT-TOKEN": self.jwt_token}
        with self.client.post("/api/v1/customer/verify", headers=headers, name="/verify", catch_response=True) as response:
            if response.status_code != 200:
                response.failure(f"Verification failed with status {response.status_code}")
            else:
                response.success()
        
        # 이 태스크는 한 번만 실행되어야 하므로, 실행 후 중단
        self.stop(True)
