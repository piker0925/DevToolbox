package com.back.user.repository;

import com.back.user.entity.RefreshTokenTheftEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface RefreshTokenTheftEventRepository extends JpaRepository<RefreshTokenTheftEvent, Long> {

    long countByUserId(Long userId);

    // 관리자 유저 목록에서 유저마다 countByUserId를 따로 호출하면 N+1이 되므로, 페이지에 실린
    // 유저 id들을 한 번에 그룹핑해서 조회한다.
    @Query("select e.userId as userId, count(e) as count from RefreshTokenTheftEvent e " +
            "where e.userId in :userIds group by e.userId")
    List<UserTheftCount> countGroupedByUserIdIn(@Param("userIds") Collection<Long> userIds);

    interface UserTheftCount {
        Long getUserId();
        Long getCount();
    }
}
