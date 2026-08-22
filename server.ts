import express from "express";
import http from "http";
import path from "path";
import fs from "fs";
import { createServer as createViteServer } from "vite";
import jwt from "jsonwebtoken";
import cors from "cors";

const app = express();
const server = http.createServer(app);
const PORT = 3000;
const JWT_SECRET = "logistics_enterprise_jwt_secret_key_2026";

app.use(cors());
app.use(express.json({ limit: "20mb" }));

// -------------------------------------------------------------
// In-Memory Simulated Database Stores for Live Microservices API
// -------------------------------------------------------------

interface OrderItem {
  id: string;
  itemName: string;
  quantity: number;
  weightKg: number;
  declaredValue?: number;
  category?: string;
}

interface Order {
  id: string;
  trackingNumber: string;
  customerId: string;
  status: string;
  senderName: string;
  senderPhone: string;
  senderAddress: string;
  senderLatitude: number;
  senderLongitude: number;
  recipientName: string;
  recipientPhone: string;
  recipientAddress: string;
  recipientLatitude: number;
  recipientLongitude: number;
  totalWeightKg: number;
  totalVolumeM3?: number;
  baseShippingFee: number;
  weightSurcharge: number;
  insuranceFee: number;
  codAmount: number;
  totalAmount: number;
  specialInstructions?: string;
  assignedDriverId?: string;
  currentHubId?: string;
  items: OrderItem[];
  createdAt: string;
  updatedAt: string;
}

interface OutboxEvent {
  id: string;
  aggregateType: string;
  aggregateId: string;
  eventType: string;
  payload: any;
  processed: boolean;
  createdAt: string;
}

interface Driver {
  id: string;
  driverCode: string;
  fullName: string;
  phone: string;
  vehiclePlate: string;
  vehicleType: "MOTORBIKE" | "VAN" | "TRUCK_1T" | "TRUCK_5T";
  status: "AVAILABLE" | "ON_DUTY" | "BUSY" | "OFFLINE";
  currentLatitude: number;
  currentLongitude: number;
  assignedHubId: string;
  rating: number;
  activeDeliveriesCount: number;
}

interface PickupTask {
  id: string;
  orderId: string;
  trackingNumber: string;
  driverId: string | null;
  status: "PENDING" | "ASSIGNED" | "EN_ROUTE_TO_PICKUP" | "ARRIVED_AT_SENDER" | "PICKED_UP" | "FAILED";
  senderAddress: string;
  scheduledPickupTime: string;
  actualPickupTime?: string;
}

interface ProofOfDelivery {
  id: string;
  trackingNumber: string;
  orderId: string;
  recipientSignedName: string;
  recipientPhone: string;
  signatureDataUri?: string;
  photoEvidenceUrl?: string;
  deliveryLatitude: number;
  deliveryLongitude: number;
  courierId: string;
  result: "SUCCESS" | "FAILED_ATTEMPT" | "REJECTED_BY_RECEIVER";
  deliveredAt: string;
}

interface TrackingEvent {
  id: string;
  trackingNumber: string;
  eventType: string;
  statusDescription: string;
  locationName: string;
  latitude: number;
  longitude: number;
  actorName: string;
  actorRole: string;
  timestamp: string;
}

interface NotificationLog {
  id: string;
  recipient: string;
  channel: "SMS" | "EMAIL" | "PUSH" | "ZALO_ZNS";
  title: string;
  messageContent: string;
  trackingNumber?: string;
  status: "SENT" | "QUEUED" | "FAILED";
  sentAt: string;
}

// Initial Seed Data
const drivers: Driver[] = [
  {
    id: "drv-101",
    driverCode: "DRV-HCM-01",
    fullName: "Nguyễn Văn Tuấn",
    phone: "0908123456",
    vehiclePlate: "59B1-998.88",
    vehicleType: "MOTORBIKE",
    status: "AVAILABLE",
    currentLatitude: 10.7769,
    currentLongitude: 106.7009,
    assignedHubId: "HUB-HCM-CENTRAL",
    rating: 4.95,
    activeDeliveriesCount: 2,
  },
  {
    id: "drv-102",
    driverCode: "DRV-HCM-02",
    fullName: "Trần Minh Quang",
    phone: "0912345678",
    vehiclePlate: "51D-543.21",
    vehicleType: "VAN",
    status: "AVAILABLE",
    currentLatitude: 10.7825,
    currentLongitude: 106.6980,
    assignedHubId: "HUB-HCM-WEST",
    rating: 4.88,
    activeDeliveriesCount: 4,
  },
  {
    id: "drv-103",
    driverCode: "DRV-HN-01",
    fullName: "Lê Hoàng Nam",
    phone: "0987654321",
    vehiclePlate: "29A-123.45",
    vehicleType: "TRUCK_1T",
    status: "ON_DUTY",
    currentLatitude: 21.0285,
    currentLongitude: 105.8542,
    assignedHubId: "HUB-HN-NORTH",
    rating: 4.92,
    activeDeliveriesCount: 7,
  }
];

const orders: Order[] = [
  {
    id: "ord-8801",
    trackingNumber: "VNX99281720",
    customerId: "cust-001",
    status: "OUT_FOR_DELIVERY",
    senderName: "Công ty Cổ phần TechVN",
    senderPhone: "0901234567",
    senderAddress: "72 Lê Thánh Tôn, Bến Nghé, Quận 1, TP. Hồ Chí Minh",
    senderLatitude: 10.7769,
    senderLongitude: 106.7009,
    recipientName: "Phạm Thu Hằng",
    recipientPhone: "0988776655",
    recipientAddress: "123 Hoàng Hoa Thám, Phường 13, Tân Bình, TP. Hồ Chí Minh",
    recipientLatitude: 10.8012,
    recipientLongitude: 106.6534,
    totalWeightKg: 1.8,
    baseShippingFee: 25000,
    weightSurcharge: 0,
    insuranceFee: 15000,
    codAmount: 850000,
    totalAmount: 40000,
    specialInstructions: "Giao giờ hành chính, gọi trước khi giao",
    assignedDriverId: "drv-101",
    currentHubId: "HUB-HCM-CENTRAL",
    items: [
      { id: "item-1", itemName: "Bàn phím cơ không dây", quantity: 1, weightKg: 1.2, declaredValue: 1500000, category: "Electronics" },
      { id: "item-2", itemName: "Chuột quang công thái học", quantity: 1, weightKg: 0.6, declaredValue: 650000, category: "Accessories" }
    ],
    createdAt: new Date(Date.now() - 86400000).toISOString(),
    updatedAt: new Date().toISOString()
  }
];

const outboxEvents: OutboxEvent[] = [
  {
    id: "outbox-001",
    aggregateType: "ORDER",
    aggregateId: "ord-8801",
    eventType: "ORDER_CREATED",
    payload: { trackingNumber: "VNX99281720", status: "CREATED" },
    processed: true,
    createdAt: new Date(Date.now() - 86400000).toISOString()
  },
  {
    id: "outbox-002",
    aggregateType: "ORDER",
    aggregateId: "ord-8801",
    eventType: "ORDER_STATUS_UPDATED",
    payload: { trackingNumber: "VNX99281720", status: "OUT_FOR_DELIVERY", driverId: "drv-101" },
    processed: true,
    createdAt: new Date().toISOString()
  }
];

const pickupTasks: PickupTask[] = [
  {
    id: "pku-001",
    orderId: "ord-8801",
    trackingNumber: "VNX99281720",
    driverId: "drv-101",
    status: "PICKED_UP",
    senderAddress: "72 Lê Thánh Tôn, Bến Nghé, Quận 1, TP. Hồ Chí Minh",
    scheduledPickupTime: new Date(Date.now() - 72000000).toISOString(),
    actualPickupTime: new Date(Date.now() - 70000000).toISOString()
  }
];

const trackingEvents: TrackingEvent[] = [
  {
    id: "trk-001",
    trackingNumber: "VNX99281720",
    eventType: "CREATED",
    statusDescription: "Đơn hàng đã được tạo và gửi yêu cầu lấy hàng",
    locationName: "Kho gửi hàng Quận 1",
    latitude: 10.7769,
    longitude: 106.7009,
    actorName: "TechVN Store",
    actorRole: "CUSTOMER",
    timestamp: new Date(Date.now() - 86400000).toISOString()
  },
  {
    id: "trk-002",
    trackingNumber: "VNX99281720",
    eventType: "PICKED_UP",
    statusDescription: "Tài xế Nguyễn Văn Tuấn đã nhận kiện hàng từ người gửi",
    locationName: "72 Lê Thánh Tôn, Bến Nghé, Quận 1",
    latitude: 10.7770,
    longitude: 106.7010,
    actorName: "Nguyễn Văn Tuấn (DRV-HCM-01)",
    actorRole: "COURIER",
    timestamp: new Date(Date.now() - 70000000).toISOString()
  },
  {
    id: "trk-003",
    trackingNumber: "VNX99281720",
    eventType: "IN_SORTING_HUB",
    statusDescription: "Kiện hàng đã nhập trung tâm phân loại HUB-HCM-CENTRAL",
    locationName: "Trung tâm Khai thác Tân Bình",
    latitude: 10.7950,
    longitude: 106.6600,
    actorName: "Hệ thống Phân loại Tự động",
    actorRole: "HUB_OPERATOR",
    timestamp: new Date(Date.now() - 40000000).toISOString()
  },
  {
    id: "trk-004",
    trackingNumber: "VNX99281720",
    eventType: "OUT_FOR_DELIVERY",
    statusDescription: "Đang giao hàng đến người nhận (Dự kiến trong 30-60 phút)",
    locationName: "Tân Bình, TP. Hồ Chí Minh",
    latitude: 10.8005,
    longitude: 106.6540,
    actorName: "Nguyễn Văn Tuấn",
    actorRole: "COURIER",
    timestamp: new Date().toISOString()
  }
];

const notificationLogs: NotificationLog[] = [
  {
    id: "notif-001",
    recipient: "0988776655",
    channel: "SMS",
    title: "Thông báo giao hàng VNX99281720",
    messageContent: "Don hang VNX99281720 dang duoc tai xe Nguyen Van Tuan (0908123456) giao den ban. Tien thu ho COD: 850.000d.",
    trackingNumber: "VNX99281720",
    status: "SENT",
    sentAt: new Date().toISOString()
  }
];

const pods: ProofOfDelivery[] = [];

// ==========================================
// 1. ORDER MANAGEMENT SERVICE APIs (/api/v1/orders)
// ==========================================

app.post("/api/v1/orders", (req, res) => {
  const body = req.body;
  const trackingNumber = "VNX" + Math.floor(10000000 + Math.random() * 90000000);
  
  const weight = Number(body.totalWeightKg || 1.0);
  const baseFee = 25000;
  const weightSurcharge = weight > 2 ? (weight - 2) * 5000 : 0;
  const insuranceFee = body.declaredValue ? body.declaredValue * 0.005 : 0;
  const totalAmount = baseFee + weightSurcharge + insuranceFee;

  const newOrder: Order = {
    id: "ord-" + Date.now(),
    trackingNumber,
    customerId: body.customerId || "cust-default",
    status: "CREATED",
    senderName: body.senderName || "Người gửi mẫu",
    senderPhone: body.senderPhone || "0901234567",
    senderAddress: body.senderAddress || "TP. Hồ Chí Minh",
    senderLatitude: body.senderLatitude || 10.7769,
    senderLongitude: body.senderLongitude || 106.7009,
    recipientName: body.recipientName || "Người nhận mẫu",
    recipientPhone: body.recipientPhone || "0987654321",
    recipientAddress: body.recipientAddress || "TP. Hồ Chí Minh",
    recipientLatitude: body.recipientLatitude || 10.8012,
    recipientLongitude: body.recipientLongitude || 106.6534,
    totalWeightKg: weight,
    totalVolumeM3: body.totalVolumeM3 || 0.01,
    baseShippingFee: baseFee,
    weightSurcharge,
    insuranceFee,
    codAmount: Number(body.codAmount || 0),
    totalAmount,
    specialInstructions: body.specialInstructions || "",
    items: body.items || [{ id: "item-1", itemName: "Hàng bưu gửi tiêu chuẩn", quantity: 1, weightKg: weight }],
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString()
  };

  orders.unshift(newOrder);

  // Write Outbox Event
  outboxEvents.unshift({
    id: "outbox-" + Date.now(),
    aggregateType: "ORDER",
    aggregateId: newOrder.id,
    eventType: "ORDER_CREATED",
    payload: { trackingNumber: newOrder.trackingNumber, totalAmount: newOrder.totalAmount },
    processed: true,
    createdAt: new Date().toISOString()
  });

  // Write initial Tracking
  trackingEvents.unshift({
    id: "trk-" + Date.now(),
    trackingNumber: newOrder.trackingNumber,
    eventType: "CREATED",
    statusDescription: "Đơn hàng đã được khởi tạo thành công trên hệ thống",
    locationName: newOrder.senderAddress,
    latitude: newOrder.senderLatitude,
    longitude: newOrder.senderLongitude,
    actorName: newOrder.senderName,
    actorRole: "CUSTOMER",
    timestamp: new Date().toISOString()
  });

  return res.status(201).json({
    success: true,
    code: "201",
    message: "Tạo đơn hàng thành công và đã ghi Transactional Outbox Kafka",
    data: newOrder
  });
});

app.get("/api/v1/orders", (req, res) => {
  return res.json({
    success: true,
    code: "200",
    data: orders,
    total: orders.length
  });
});

app.get("/api/v1/orders/track/:trackingNumber", (req, res) => {
  const { trackingNumber } = req.params;
  const order = orders.find(o => o.trackingNumber.toUpperCase() === trackingNumber.toUpperCase());
  if (!order) {
    return res.status(404).json({ success: false, code: "404", message: "Không tìm thấy mã vận đơn" });
  }
  return res.json({ success: true, code: "200", data: order });
});

app.put("/api/v1/orders/:orderId/status", (req, res) => {
  const { orderId } = req.params;
  const { status, assignedDriverId, currentHubId, note } = req.body;
  const order = orders.find(o => o.id === orderId || o.trackingNumber === orderId);
  if (!order) {
    return res.status(404).json({ success: false, code: "404", message: "Không tìm thấy đơn hàng" });
  }

  order.status = status;
  if (assignedDriverId) order.assignedDriverId = assignedDriverId;
  if (currentHubId) order.currentHubId = currentHubId;
  order.updatedAt = new Date().toISOString();

  // Outbox
  outboxEvents.unshift({
    id: "outbox-" + Date.now(),
    aggregateType: "ORDER",
    aggregateId: order.id,
    eventType: "ORDER_STATUS_UPDATED",
    payload: { trackingNumber: order.trackingNumber, status, note },
    processed: true,
    createdAt: new Date().toISOString()
  });

  // Tracking log
  trackingEvents.unshift({
    id: "trk-" + Date.now(),
    trackingNumber: order.trackingNumber,
    eventType: status,
    statusDescription: note || `Cập nhật trạng thái: ${status}`,
    locationName: currentHubId || "Tuyến vận chuyển",
    latitude: order.senderLatitude,
    longitude: order.senderLongitude,
    actorName: assignedDriverId || "Hệ thống Quản lý Vận tải",
    actorRole: "SYSTEM",
    timestamp: new Date().toISOString()
  });

  return res.json({ success: true, code: "200", message: "Cập nhật trạng thái đơn hàng thành công", data: order });
});

app.post("/api/v1/orders/calculate-price", (req, res) => {
  const { weightKg = 1.0, distanceKm = 5.0, declaredValue = 0, codAmount = 0, expressDelivery = false } = req.body;
  const baseFee = 25000;
  const distanceSurcharge = Math.max(1, distanceKm) * 4000;
  const weightSurcharge = weightKg > 2 ? (weightKg - 2) * 5000 : 0;
  const insuranceFee = declaredValue > 0 ? declaredValue * 0.005 : 0;
  const codFee = codAmount > 0 ? 5000 : 0;
  let total = baseFee + distanceSurcharge + weightSurcharge + insuranceFee + codFee;
  if (expressDelivery) total *= 1.3;

  return res.json({
    success: true,
    code: "200",
    data: {
      baseFee,
      distanceSurcharge,
      weightSurcharge,
      insuranceFee,
      codFee,
      totalShippingFee: Math.round(total),
      currency: "VND",
      estimatedDeliveryHours: expressDelivery ? "4 - 8 giờ" : "24 - 48 giờ"
    }
  });
});

// ==========================================
// 2. PICKUP & FLEET SERVICE APIs (/api/v1/fleet)
// ==========================================

app.get("/api/v1/fleet/drivers", (_req, res) => {
  return res.json({ success: true, code: "200", data: drivers });
});

app.post("/api/v1/fleet/drivers", (req, res) => {
  const body = req.body;
  const newDriver: Driver = {
    id: "drv-" + Date.now(),
    driverCode: body.driverCode || "DRV-NEW-" + Math.floor(100 + Math.random() * 900),
    fullName: body.fullName || "Tài xế mới",
    phone: body.phone || "0900000000",
    vehiclePlate: body.vehiclePlate || "59A-999.99",
    vehicleType: body.vehicleType || "MOTORBIKE",
    status: "AVAILABLE",
    currentLatitude: body.currentLatitude || 10.7769,
    currentLongitude: body.currentLongitude || 106.7009,
    assignedHubId: body.assignedHubId || "HUB-HCM-CENTRAL",
    rating: 5.0,
    activeDeliveriesCount: 0
  };
  drivers.push(newDriver);
  return res.status(201).json({ success: true, code: "201", data: newDriver });
});

app.get("/api/v1/fleet/pickups", (_req, res) => {
  return res.json({ success: true, code: "200", data: pickupTasks });
});

app.post("/api/v1/fleet/pickups/:taskId/assign/:driverId", (req, res) => {
  const { taskId, driverId } = req.params;
  const task = pickupTasks.find(t => t.id === taskId);
  const driver = drivers.find(d => d.id === driverId);

  if (!task || !driver) {
    return res.status(404).json({ success: false, code: "404", message: "Task hoặc Driver không tồn tại" });
  }

  task.driverId = driver.id;
  task.status = "ASSIGNED";
  driver.status = "BUSY";
  driver.activeDeliveriesCount += 1;

  return res.json({ success: true, code: "200", message: "Điều phối tài xế thành công", data: { task, driver } });
});

// ==========================================
// 3. FULFILLMENT & POD APIs (/api/v1/fulfillment)
// ==========================================

app.post("/api/v1/fulfillment/pod", (req, res) => {
  const body = req.body;
  const newPod: ProofOfDelivery = {
    id: "pod-" + Date.now(),
    trackingNumber: body.trackingNumber,
    orderId: body.orderId || "ord-auto",
    recipientSignedName: body.recipientSignedName || "Người nhận",
    recipientPhone: body.recipientPhone || "0987654321",
    signatureDataUri: body.signatureDataUri || "data:image/svg+xml;utf8,<svg>Signature</svg>",
    photoEvidenceUrl: body.photoEvidenceUrl || "https://images.unsplash.com/photo-1549465220-1a8b9238cd48?w=500",
    deliveryLatitude: body.deliveryLatitude || 10.8012,
    deliveryLongitude: body.deliveryLongitude || 106.6534,
    courierId: body.courierId || "drv-101",
    result: "SUCCESS",
    deliveredAt: new Date().toISOString()
  };

  pods.unshift(newPod);

  // Update corresponding order
  const order = orders.find(o => o.trackingNumber === body.trackingNumber);
  if (order) {
    order.status = "DELIVERED";
    order.updatedAt = new Date().toISOString();
  }

  // Tracking log
  trackingEvents.unshift({
    id: "trk-" + Date.now(),
    trackingNumber: body.trackingNumber,
    eventType: "DELIVERED",
    statusDescription: `Đã giao hàng thành công. Ký nhận bởi: ${newPod.recipientSignedName}`,
    locationName: "Địa chỉ người nhận",
    latitude: newPod.deliveryLatitude,
    longitude: newPod.deliveryLongitude,
    actorName: newPod.courierId,
    actorRole: "COURIER",
    timestamp: new Date().toISOString()
  });

  return res.status(201).json({ success: true, code: "201", message: "Ghi nhận Proof of Delivery (POD) thành công", data: newPod });
});

app.get("/api/v1/fulfillment/pod/:trackingNumber", (req, res) => {
  const { trackingNumber } = req.params;
  const pod = pods.find(p => p.trackingNumber === trackingNumber);
  if (!pod) return res.status(404).json({ success: false, code: "404", message: "Chưa có POD cho vận đơn này" });
  return res.json({ success: true, code: "200", data: pod });
});

// ==========================================
// 4. REALTIME TRACKING APIs (/api/v1/tracking)
// ==========================================

app.get("/api/v1/tracking/:trackingNumber", (req, res) => {
  const { trackingNumber } = req.params;
  const history = trackingEvents.filter(t => t.trackingNumber.toUpperCase() === trackingNumber.toUpperCase());
  return res.json({
    success: true,
    code: "200",
    trackingNumber,
    timeline: history
  });
});

app.post("/api/v1/tracking/events", (req, res) => {
  const body = req.body;
  const newEvent: TrackingEvent = {
    id: "trk-" + Date.now(),
    trackingNumber: body.trackingNumber,
    eventType: body.eventType || "GPS_PING",
    statusDescription: body.statusDescription || "Cập nhật vị trí di chuyển thực tế",
    locationName: body.locationName || "Tọa độ vệ tinh",
    latitude: body.latitude || 10.78,
    longitude: body.longitude || 106.69,
    actorName: body.actorName || "Hệ thống Telemetry",
    actorRole: body.actorRole || "COURIER",
    timestamp: new Date().toISOString()
  };
  trackingEvents.unshift(newEvent);
  return res.status(201).json({ success: true, code: "201", data: newEvent });
});

// ==========================================
// 5. NOTIFICATION APIs (/api/v1/notifications)
// ==========================================

app.get("/api/v1/notifications/logs", (_req, res) => {
  return res.json({ success: true, code: "200", data: notificationLogs });
});

app.post("/api/v1/notifications/send-manual", (req, res) => {
  const body = req.body;
  const newLog: NotificationLog = {
    id: "notif-" + Date.now(),
    recipient: body.recipient || "0900000000",
    channel: body.channel || "SMS",
    title: body.title || "Thông báo Logistics",
    messageContent: body.messageContent || "Nội dung tin nhắn SMS/Email thông báo.",
    trackingNumber: body.trackingNumber,
    status: "SENT",
    sentAt: new Date().toISOString()
  };
  notificationLogs.unshift(newLog);
  return res.status(201).json({ success: true, code: "201", data: newLog });
});

// ==========================================
// 6. IAM AUTH APIs (/api/v1/auth)
// ==========================================

app.post("/api/v1/auth/login", (req, res) => {
  const { usernameOrEmail, password } = req.body;
  const token = jwt.sign({ username: usernameOrEmail, role: "ROLE_ADMIN" }, JWT_SECRET, { expiresIn: "24h" });
  return res.json({
    success: true,
    code: "200",
    data: {
      accessToken: token,
      tokenType: "Bearer",
      userId: "user-admin-001",
      username: usernameOrEmail || "admin",
      email: "admin@logistics.vn",
      role: "ROLE_ADMIN",
      fullName: "Hệ Thống Quản Trị Trung Tâm"
    }
  });
});

// ==========================================
// 7. SYSTEM ACTUATOR & OBSERVABILITY METRICS
// ==========================================

app.get("/actuator/health", (_req, res) => {
  res.json({
    status: "UP",
    components: {
      db: { status: "UP", details: { database: "PostgreSQL 16", connectionsActive: 14 } },
      redis: { status: "UP", details: { version: "7.2.4", mode: "standalone" } },
      kafka: { status: "UP", details: { clusterId: "logistics-kafka-cluster", brokers: 3 } },
      discoveryClient: { status: "UP", details: { services: ["order-service", "pickup-fleet-service", "fulfillment-service", "tracking-service", "notification-service", "user-auth-service"] } },
      circuitBreakers: { status: "UP", details: { state: "CLOSED", bufferedCalls: 100, failureRate: "0.0%" } }
    }
  });
});

app.get("/actuator/prometheus", (_req, res) => {
  res.type("text/plain");
  res.send(`# HELP http_server_requests_seconds Duration of HTTP requests
# TYPE http_server_requests_seconds summary
http_server_requests_seconds_count{method="POST",uri="/api/v1/orders",status="201"} 1420
http_server_requests_seconds_sum{method="POST",uri="/api/v1/orders",status="201"} 78.4
# HELP jvm_memory_used_bytes The amount of used memory
# TYPE jvm_memory_used_bytes gauge
jvm_memory_used_bytes{area="heap"} 268435456
# HELP kafka_consumer_fetch_manager_records_consumed_total The total number of records consumed
# TYPE kafka_consumer_fetch_manager_records_consumed_total counter
kafka_consumer_fetch_manager_records_consumed_total 48520
`);
});

// ==========================================
// 8. SPRING BOOT SOURCE CODE INSPECTOR API
// ==========================================

app.get("/api/v1/microservices/architecture", (_req, res) => {
  res.json({
    platformName: "Enterprise Logistics & Delivery Microservices Platform",
    runtime: "Java 21 LTS + Spring Boot 3.4.2 + Node 22",
    frameworks: [
      "Spring Cloud 2024.0.0 (Gateway, Eureka, OpenFeign)",
      "Spring Data JPA + PostgreSQL 16 (HikariCP)",
      "Spring Data Redis + Redisson (Distributed Locks)",
      "Spring Kafka + Transactional Outbox Pattern",
      "Resilience4j (Circuit Breakers, Rate Limiter)",
      "Springdoc-OpenAPI 3 (Swagger)",
      "Micrometer + Prometheus + Grafana + OpenTelemetry"
    ],
    microservices: [
      {
        name: "Order Management Service",
        port: 8081,
        path: "microservices/order-service",
        storage: "PostgreSQL (Core Orders), Redis (Rate cache)",
        events: ["ORDER_CREATED", "ORDER_STATUS_UPDATED", "ORDER_CANCELLED"],
        features: ["Address Validation", "Dynamic Price Calculation", "Transactional Outbox Pattern"]
      },
      {
        name: "Pickup & Fleet Dispatch Service",
        port: 8082,
        path: "microservices/pickup-fleet-service",
        storage: "PostgreSQL (Drivers, Tasks), Redis (Driver State)",
        events: ["PICKUP_ASSIGNED", "DRIVER_LOCATION_PING"],
        features: ["Euclidean Driver Matcher", "Batch Pickups", "State Locking"]
      },
      {
        name: "Delivery Fulfillment Service",
        port: 8083,
        path: "microservices/fulfillment-service",
        storage: "PostgreSQL (Hub Transits, Proof of Deliveries)",
        events: ["HUB_SCANNED", "PACKAGE_DELIVERED"],
        features: ["Proof of Delivery (POD)", "Digital Signature Capture", "Hub Sorting Checkpoints"]
      },
      {
        name: "Realtime Tracking Service",
        port: 8084,
        path: "microservices/tracking-service",
        storage: "Redis Geospatial, PostgreSQL (Timeline)",
        events: ["TRACKING_EVENT_RECORDED"],
        features: ["Real-time GPS Coordinate Ingestion", "Redis Geo Indexing", "Journey Timeline"]
      },
      {
        name: "Notification Service",
        port: 8085,
        path: "microservices/notification-service",
        storage: "PostgreSQL (Notification Logs), Redis Queue",
        events: ["SMS_SENT", "EMAIL_DISPATCHED", "PUSH_SENT"],
        features: ["Kafka Event Consumer", "Async SMS / Email / Push", "Retry Handlers"]
      },
      {
        name: "User & IAM Authentication Service",
        port: 8080,
        path: "microservices/user-auth-service",
        storage: "PostgreSQL (Users, Roles), Redis (Sessions)",
        events: ["USER_AUTHENTICATED", "TOKEN_REFRESHED"],
        features: ["Spring Security 6", "JWT AuthN/AuthZ", "RBAC"]
      },
      {
        name: "Spring Cloud API Gateway",
        port: 8000,
        path: "microservices/api-gateway",
        storage: "Redis (Reactive Rate Limiter)",
        features: ["Dynamic Eureka Service Routing", "Global CORS Filter", "Token Relay"]
      },
      {
        name: "Service Registry (Eureka)",
        port: 8761,
        path: "microservices/service-registry",
        features: ["Service Discovery", "Heartbeat Health Monitoring", "Dynamic IP Resolution"]
      }
    ],
    outboxEventsCount: outboxEvents.length,
    activeOrdersCount: orders.length,
    activeDriversCount: drivers.length
  });
});

app.get("/api/v1/microservices/files", (_req, res) => {
  const rootDir = process.cwd();
  function getJavaFiles(dir: string): { relativePath: string; size: number; content: string }[] {
    let results: { relativePath: string; size: number; content: string }[] = [];
    if (!fs.existsSync(dir)) return results;
    const list = fs.readdirSync(dir);
    list.forEach(file => {
      const fullPath = path.join(dir, file);
      const stat = fs.statSync(fullPath);
      if (stat && stat.isDirectory()) {
        if (file !== "node_modules" && file !== ".git" && file !== "dist") {
          results = results.concat(getJavaFiles(fullPath));
        }
      } else if (file.endsWith(".java") || file.endsWith(".xml") || file.endsWith(".yml") || file.endsWith(".yaml")) {
        const relativePath = path.relative(rootDir, fullPath).replace(/\\/g, "/");
        const content = fs.readFileSync(fullPath, "utf-8");
        results.push({ relativePath, size: stat.size, content });
      }
    });
    return results;
  }
  const files = getJavaFiles(path.join(rootDir, "microservices"));
  res.json({ count: files.length, files });
});

// ==========================================
// VITE SPA MIDDLEWARE / PRODUCTION STATIC
// ==========================================

async function startServer() {
  if (process.env.NODE_ENV !== "production") {
    const vite = await createViteServer({
      server: { middlewareMode: true },
      appType: "spa",
    });
    app.use(vite.middlewares);
  } else {
    const distPath = path.join(process.cwd(), "dist");
    app.use(express.static(distPath));
    app.get("*", (_req, res) => {
      res.sendFile(path.join(distPath, "index.html"));
    });
  }

  server.listen(PORT, "0.0.0.0", () => {
    console.log(`[Enterprise Backend] Running on http://localhost:${PORT}`);
    console.log(`[Actuator Health] http://localhost:${PORT}/actuator/health`);
    console.log(`[Prometheus Metrics] http://localhost:${PORT}/actuator/prometheus`);
    console.log(`[Order API] http://localhost:${PORT}/api/v1/orders`);
  });
}

startServer();
