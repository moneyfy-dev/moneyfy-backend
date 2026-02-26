package com.referidos.app.segurosref.repositories;

import java.util.List;
import java.util.Optional;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.referidos.app.segurosref.models.DeviceModel;

@Repository
public interface DeviceRepository extends MongoRepository<DeviceModel, ObjectId> {

    Optional<DeviceModel> findByUser(String user);
    Optional<DeviceModel> findByUserAndDevice(String user, String device);
    Optional<DeviceModel> findByDeviceAndRefreshToken(String device, String refreshToken);
    Optional<DeviceModel> findByRefreshToken(String refreshToken);
    List<DeviceModel> findAllByUserContaining(String user);
    boolean existsByDevice(String device);

}
