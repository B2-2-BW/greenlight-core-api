

import asyncio
import aiohttp
import json
import time
from aiohttp_sse_client.client import EventSource

# --- CONFIGURATION ---
# API 엔드포인트 URL을 설정하세요.
URL_CHECK_OR_ENTER = "https://api.greenlight-core.winten.im/api/v1/queue/check-or-enter"
URL_SSE = "https://api.greenlight-core.winten.im/waiting/sse"
URL_VERIFY = "https://api.greenlight-core.winten.im/api/v1/customer/verify"

# 동시에 실행할 클라이언트(사용자) 수를 설정하세요.
NUM_CLIENTS = 3
# 테스트할 대기열의 Action ID를 설정하세요.
ACTION_ID = 1
DESTINATION_URL = "/items/test-item-123"
# SSE 대기 타임아웃 (초)
SSE_TIMEOUT = 180
# --- END CONFIGURATION ---

# --- 통계 ---
stats = {
    "completed": 0,
    "immediate_entry": 0,
    "entered_after_wait": 0,
    "failures": 0,
    "sse_timeouts": 0
}

async def run_client(session, client_id):
    """한 명의 사용자에 대한 전체 테스트 흐름을 시뮬레이션합니다."""
    try:
        # --- 1단계: 대기열 진입 요청 (check-or-enter) ---
        print(f"[Client {client_id}] 1. 대기열 진입 요청...")
        payload = {"actionId": ACTION_ID, "destinationUrl": DESTINATION_URL}
        async with session.post(URL_CHECK_OR_ENTER, json=payload) as response:
            if response.status != 200:
                print(f"[Client {client_id}] 실패: 1단계 check-or-enter 실패 (Status: {response.status})")
                stats["failures"] += 1
                return

            entry_ticket = await response.json()
            token = entry_ticket.get("token")
            customer_id = entry_ticket.get("customerId")
            wait_status = entry_ticket.get("waitStatus")
            
            print(f"[Client {client_id}] 1. 진입 응답: Status={wait_status}, CustomerId={customer_id}")

        if wait_status == "ENTER":
            print(f"[Client {client_id}] 즉시 입장 완료.")
            stats["immediate_entry"] += 1
            stats["completed"] += 1
            return

        # --- 2단계: SSE 연결 및 대기 ---
        print(f"[Client {client_id}] 2. SSE 연결 및 대기 시작...")
        sse_url = f"{URL_SSE}?actionId={ACTION_ID}&customerId={customer_id}"
        
        try:
            async with EventSource(sse_url, session=session, timeout=SSE_TIMEOUT) as event_source:
                async for event in event_source:
                    if event.type == "message":
                        queue_info = json.loads(event.data)
                        current_status = queue_info.get("waitStatus")
                        print(f"[Client {client_id}] 2. SSE 수신: Status={current_status}, Rank={queue_info.get('rank')}")
                        if current_status == "READY":
                            print(f"[Client {client_id}] 2. SSE 'READY' 상태 수신. 3단계로 진행.")
                            stats["entered_after_wait"] += 1
                            break
        except asyncio.TimeoutError:
            print(f"[Client {client_id}] 실패: 2단계 SSE 대기 시간 초과.")
            stats["sse_timeouts"] += 1
            stats["failures"] += 1
            return
        except Exception as e:
            print(f"[Client {client_id}] 실패: 2단계 SSE 연결 중 오류 - {e}")
            stats["failures"] += 1
            return

        # --- 3단계: 입장권 검증 요청 (verify) ---
        print(f"[Client {client_id}] 3. 최종 입장 검증 요청...")
        headers = {"X-GREENLIGHT-TOKEN": token}
        async with session.post(URL_VERIFY, headers=headers) as response:
            if response.status == 200:
                verification_response = await response.json()
                print(f"[Client {client_id}] 3. 검증 성공! Response: {verification_response}")
                stats["completed"] += 1
            else:
                print(f"[Client {client_id}] 실패: 3단계 검증 실패 (Status: {response.status})")
                stats["failures"] += 1

    except aiohttp.ClientError as e:
        print(f"[Client {client_id}] 실패: 네트워크 오류 - {e}")
        stats["failures"] += 1
    except Exception as e:
        print(f"[Client {client_id}] 실패: 예상치 못한 오류 - {e}")
        stats["failures"] += 1


async def main():
    """메인 함수: aiohttp 세션을 생성하고 모든 클라이언트 태스크를 실행합니다."""
    start_time = time.time()
    async with aiohttp.ClientSession() as session:
        print(f"--- {NUM_CLIENTS}명의 클라이언트로 부하 테스트를 시작합니다. ---")
        tasks = [run_client(session, i) for i in range(1, NUM_CLIENTS + 1)]
        await asyncio.gather(*tasks)

    end_time = time.time()
    duration = end_time - start_time
    
    print("\n--- 부하 테스트 종료 ---")
    print(f"총 실행 시간: {duration:.2f}초")
    print(f"성공: {stats['completed']} (즉시 입장: {stats['immediate_entry']}, 대기 후 입장: {stats['entered_after_wait']})")
    print(f"실패: {stats['failures']} (SSE 타임아웃: {stats['sse_timeouts']})")
    print("----------------------")


if __name__ == "__main__":
    asyncio.run(main())
