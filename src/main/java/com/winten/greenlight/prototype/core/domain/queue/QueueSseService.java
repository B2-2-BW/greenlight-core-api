package com.winten.greenlight.prototype.core.domain.queue;

import com.winten.greenlight.prototype.core.db.repository.redis.queue.QueueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
@RequiredArgsConstructor
public class QueueSseService {
    private final QueueRepository queueRepository;

    // 참고자료: Flux.interval 사용 시 모든 사용자 요청에 interval 이 생성됨
    // 사용자 요청은 연결만 진행하고 일정 주기로 전체 사용자에게 일괄로 상태를 전송하는 방식으로 구현 필요
    // https://happyzodiac.tistory.com/90
    // https://why-doing.tistory.com/136

    // sse 연결해서 고객의 현재 대기상태를 조회할 때 사용
    public Flux<?> connect(String entryId) {
        return null;
    }

    public void updateQueueStatus() {
    }
}