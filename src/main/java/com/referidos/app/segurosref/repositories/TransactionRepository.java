package com.referidos.app.segurosref.repositories;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.referidos.app.segurosref.models.TransactionModel;

@Repository
public interface TransactionRepository extends MongoRepository<TransactionModel, String> {

    @Query(value = "{ 'userId': ?0, 'commissionScope': { $gte: ?1 }, 'status': { $in: ['Aprobado', 'Confirmando', 'Liberado'] } }", count = true)
    long countByUserIdAndCommissionScopeGTEAndStatusPassed(String userId, int commissionScope);
    
    Optional<TransactionModel> findByUserIdAndQuoterId(String userId, String quoterId);
    boolean existsByUserIdAndQuoterId(String userId, String quoterId);
    
    // Consulta para realizar la lógica del reporte de comisiones
    @Query(value = "{ 'approvalDate': { $lt: ?0 }, 'status': { $in: ['Aprobado', 'Confirmando'] } }")
    List<TransactionModel> findAllByApprovalDateBeforeAndStatusProcessing(LocalDateTime afterCutoffDate);

    @Query(value = "{ 'approvalDate': { $gte: ?0 }, 'status': { $in: ['Aprobado', 'Confirmando', 'Liberado'] } }")
    List<TransactionModel> findAllByApprovalDateAfterAndStatusAccepted(LocalDateTime lastMonthlyEarning);

}
