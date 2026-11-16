package com.wikicook.profile.repository;

import com.wikicook.profile.domain.DietaryNotice;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Notices of one owner (FR-019). Every method takes the owner's id: nobody reads or closes another person's. */
public interface DietaryNoticeRepository extends JpaRepository<DietaryNotice, Long> {

    List<DietaryNotice> findByUserIdOrderByCreatedAtDescIdDesc(long userId);

    /** Closes the notices {@code ids} that belong to {@code userId}; ids of other people are ignored. */
    @Modifying
    @Query("delete from DietaryNotice n where n.userId = :userId and n.id in :ids")
    int deleteByUserIdAndIdIn(@Param("userId") long userId, @Param("ids") Collection<Long> ids);
}
