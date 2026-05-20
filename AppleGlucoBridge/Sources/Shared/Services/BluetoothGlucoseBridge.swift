import CoreBluetooth
import Foundation

enum BluetoothGlucoseBridgeConstants {
    static let localName = "GlucoBridge"
    static let serviceUUID = CBUUID(string: "6F5A1000-BD38-4B9A-9A1F-8A9F2A120001")
    static let snapshotCharacteristicUUID = CBUUID(string: "6F5A1001-BD38-4B9A-9A1F-8A9F2A120001")
}

final class BluetoothGlucoseBridge: NSObject {
    static let shared = BluetoothGlucoseBridge()

    private var peripheralManager: CBPeripheralManager?
    private var snapshotCharacteristic: CBMutableCharacteristic?
    private var latestPayload = Data()
    private var isAdvertising = false

    private override init() {
        super.init()
        peripheralManager = CBPeripheralManager(
            delegate: self,
            queue: nil,
            options: [CBPeripheralManagerOptionShowPowerAlertKey: true]
        )
    }

    var stateDescription: String {
        guard let peripheralManager else {
            return "Bluetooth manager is not available yet."
        }

        switch peripheralManager.state {
        case .unknown:
            return "Bluetooth state is still initializing."
        case .resetting:
            return "Bluetooth is resetting."
        case .unsupported:
            return "This iPhone does not support BLE peripheral mode."
        case .unauthorized:
            return "Bluetooth permission is not granted."
        case .poweredOff:
            return "Bluetooth is turned off on the iPhone."
        case .poweredOn:
            return isAdvertising
                ? "BLE bridge is advertising and ready for the watch to connect."
                : "Bluetooth is on, waiting to start the BLE bridge."
        @unknown default:
            return "Bluetooth reported an unknown state."
        }
    }

    func publish(snapshot: GlucoseSnapshot) {
        latestPayload = snapshot.blePayloadData
        configureGattIfNeeded()
        restartAdvertisingIfPossible()
        pushNotificationIfPossible()
    }

    private func configureGattIfNeeded() {
        guard let peripheralManager, peripheralManager.state == .poweredOn else {
            return
        }

        if snapshotCharacteristic != nil {
            return
        }

        let characteristic = CBMutableCharacteristic(
            type: BluetoothGlucoseBridgeConstants.snapshotCharacteristicUUID,
            properties: [.read, .notify],
            value: nil,
            permissions: [.readable]
        )

        let service = CBMutableService(
            type: BluetoothGlucoseBridgeConstants.serviceUUID,
            primary: true
        )
        service.characteristics = [characteristic]

        peripheralManager.removeAllServices()
        peripheralManager.add(service)
        snapshotCharacteristic = characteristic
    }

    private func restartAdvertisingIfPossible() {
        guard let peripheralManager, peripheralManager.state == .poweredOn else {
            return
        }

        if peripheralManager.isAdvertising {
            peripheralManager.stopAdvertising()
        }

        peripheralManager.startAdvertising([
            CBAdvertisementDataLocalNameKey: BluetoothGlucoseBridgeConstants.localName,
            CBAdvertisementDataServiceUUIDsKey: [BluetoothGlucoseBridgeConstants.serviceUUID]
        ])
    }

    private func pushNotificationIfPossible() {
        guard
            let peripheralManager,
            peripheralManager.state == .poweredOn,
            let snapshotCharacteristic
        else {
            return
        }

        _ = peripheralManager.updateValue(
            latestPayload,
            for: snapshotCharacteristic,
            onSubscribedCentrals: nil
        )
    }
}

extension BluetoothGlucoseBridge: CBPeripheralManagerDelegate {
    func peripheralManagerDidUpdateState(_ peripheral: CBPeripheralManager) {
        if peripheral.state == .poweredOn {
            configureGattIfNeeded()
            restartAdvertisingIfPossible()
            pushNotificationIfPossible()
        } else {
            isAdvertising = false
        }
    }

    func peripheralManager(_ peripheral: CBPeripheralManager, didAdd service: CBService, error: Error?) {
        if let error {
            print("Failed to add BLE service: \(error.localizedDescription)")
        }
    }

    func peripheralManagerDidStartAdvertising(_ peripheral: CBPeripheralManager, error: Error?) {
        if let error {
            isAdvertising = false
            print("Failed to start BLE advertising: \(error.localizedDescription)")
            return
        }

        isAdvertising = true
    }

    func peripheralManager(_ peripheral: CBPeripheralManager, didReceiveRead request: CBATTRequest) {
        guard request.characteristic.uuid == BluetoothGlucoseBridgeConstants.snapshotCharacteristicUUID else {
            peripheral.respond(to: request, withResult: .attributeNotFound)
            return
        }

        guard request.offset <= latestPayload.count else {
            peripheral.respond(to: request, withResult: .invalidOffset)
            return
        }

        request.value = latestPayload.subdata(in: request.offset..<latestPayload.count)
        peripheral.respond(to: request, withResult: .success)
    }

    func peripheralManagerIsReady(toUpdateSubscribers peripheral: CBPeripheralManager) {
        pushNotificationIfPossible()
    }
}
