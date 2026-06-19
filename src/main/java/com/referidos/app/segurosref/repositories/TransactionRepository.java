package com.referidos.app.segurosref.repositories;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import com.referidos.app.segurosref.models.TransactionModel;

public interface TransactionRepository extends MongoRepository<TransactionModel, String> {

    @Query(value = "{'userId': ?0, 'commissionScope': {$gte: ?1}, 'status': {$in: ['Aprobado', 'Pagado']}}", count = true)
    long countByUserIdAndCommissionScopeGTEAndStatusPassed(String userId, int commissionScope);

    Optional<TransactionModel> findByUserIdAndQuoterId(String userId, String quoterId);

    Optional<TransactionModel> findByUserIdAndQuoterIdAndStatus(String userId, String quoterId, String status);

    boolean existsByUserIdAndQuoterId(String userId, String quoterId);

    // Optimizacion: extraer transacciones masivamente por lista de ids
    List<TransactionModel> findByQuoterIdIn(List<String> quoterIds);

    @Query(value = "{'approvalDate': {$lt: ?0}, 'status': 'Aprobado'}")
    List<TransactionModel> findAllByApprovalDateBeforeAndStatusApproved(LocalDateTime afterCutoffDate);

    @Query(value = "{'approvalDate': {$gte: ?0, $lte: ?1}, 'status': ?2}")
    List<TransactionModel> findAllByApprovalDateBetweenAndStatus(LocalDateTime from, LocalDateTime to, String status);

    @Query(value = "{'approvalDate': {$gte: ?0}, 'commissions.userId': ?1, 'status': {$in: ['Aprobado', 'Pagado']}}")
    List<TransactionModel> findAllByApprovalDateAfterAndCommissions_UserIdAndStatusPassed(
            LocalDateTime lastMonthlyEarning, String userId);

    @Query(value = "{'commissions.userId': ?0, 'status': {$in: ['Aprobado', 'Pagado']}}")
    List<TransactionModel> findAllByCommissions_UserIdAndStatusPassed(String userId);

    List<TransactionModel> findAllByUserReferringFound(Boolean userReferringFound);

    // Spring entiende que debe buscar dentro de la lista 'commissions', cualquier
    // objeto cuyo 'userId' coincida con el parámetro.
    List<TransactionModel> findAllByCommissions_UserId(String userId);

}
