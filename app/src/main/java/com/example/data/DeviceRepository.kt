package com.example.data

import com.example.model.DeviceEntity
import kotlinx.coroutines.flow.Flow

class DeviceRepository(private val deviceDao: DeviceDao) {
    val allDevices: Flow<List<DeviceEntity>> = deviceDao.getAllDevices()

    suspend fun insert(device: DeviceEntity) {
        deviceDao.insertDevice(device)
    }

    suspend fun update(device: DeviceEntity) {
        deviceDao.updateDevice(device)
    }

    suspend fun delete(id: Int) {
        deviceDao.deleteDevice(id)
    }
}
