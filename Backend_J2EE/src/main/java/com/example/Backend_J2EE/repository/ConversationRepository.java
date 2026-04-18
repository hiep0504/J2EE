package com.example.Backend_J2EE.repository;

import com.example.Backend_J2EE.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Integer> {

    List<Conversation> findByStatusOrderByLastMessageAtDesc(Conversation.Status status);

    List<Conversation> findByAdmin_IdAndStatusOrderByLastMessageAtDesc(Integer adminId, Conversation.Status status);

    Optional<Conversation> findTopByUser_IdAndStatusInOrderByLastMessageAtDesc(Integer userId, Collection<Conversation.Status> statuses);

        Optional<Conversation> findTopByUser_IdOrderByLastMessageAtDesc(Integer userId);

        List<Conversation> findByUser_IdOrderByLastMessageAtDesc(Integer userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Conversation c where c.id = :id")
    Optional<Conversation> findByIdForUpdate(@Param("id") Integer id);

    @Query("""
            select c from Conversation c
            where c.status = :status
              and c.lastMessageSenderType = :senderType
              and c.lastMessageAt <= :cutoff
              and (c.lastAdminReplyAt is null or c.lastAdminReplyAt < c.lastMessageAt)
            """)
    List<Conversation> findTimedOutConversations(
            @Param("status") Conversation.Status status,
            @Param("senderType") Conversation.SenderType senderType,
            @Param("cutoff") LocalDateTime cutoff
    );

    List<Conversation> findByAdmin_IdAndStatus(Integer adminId, Conversation.Status status);
}
