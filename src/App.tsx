import React, { useState } from 'react';
import {
  Server,
  Activity,
  Truck,
  Package,
  MapPin,
  Bell,
  ShieldCheck,
  Radio,
  Layers,
  Terminal,
  Play,
  CheckCircle2,
  AlertTriangle,
  RefreshCw,
  Database,
  Cpu,
  Search,
  PlusCircle,
  ExternalLink,
  ChevronRight,
  Download,
  Copy,
  Check,
  FileCode,
  Boxes,
  Cloud,
  GitBranch
} from 'lucide-react';
import { DevOpsLab } from './components/DevOpsLab';

interface MicroserviceInfo {
  id: string;
  name: string;
  port: number;
  description: string;
  tech: string[];
  status: 'UP' | 'SYNCING' | 'STANDBY';
  endpoints: string[];
}

const SERVICES: MicroserviceInfo[] = [
  {
    id: 'eureka',
    name: 'Netflix Eureka Registry',
    port: 8761,
    description: 'Dynamic service discovery, health heartbeats & cluster node registration',
    tech: ['Spring Cloud Eureka', 'Java 17', 'Port 8761'],
    status: 'UP',
    endpoints: ['/eureka/apps', '/actuator/health'],
  },
  {
    id: 'gateway',
    name: 'Spring Cloud API Gateway',
    port: 8000,
    description: 'Unified entry point, JWT validation, rate limiting & dynamic routing',
    tech: ['Spring Cloud Gateway', 'Netty WebFlux', 'Port 8000'],
    status: 'UP',
    endpoints: ['/api/v1/orders', '/api/v1/tracking', '/api/v1/auth'],
  },
  {
    id: 'user-auth',
    name: 'User & Authentication IAM',
    port: 8080,
    description: 'JWT issuance, OAuth2/RBAC, driver/merchant authorization',
    tech: ['Spring Security 6', 'JWT', 'PostgreSQL'],
    status: 'UP',
    endpoints: ['/api/v1/auth/login', '/api/v1/auth/register', '/api/v1/users'],
  },
  {
    id: 'order',
    name: 'Order Management Service',
    port: 8081,
    description: 'Order lifecycle, Outbox transactional event publishing, pricing engine',
    tech: ['Transactional Outbox', 'Kafka Producer', 'Redisson Lock'],
    status: 'UP',
    endpoints: ['/api/v1/orders', '/api/v1/orders/{code}', '/api/v1/orders/quote'],
  },
  {
    id: 'pickup',
    name: 'Pickup & Fleet Dispatch',
    port: 8082,
    description: 'Driver assignment, zone clustering, vehicle capacity routing',
    tech: ['Fleet Engine', 'Kafka Consumer', 'PostgreSQL'],
    status: 'UP',
    endpoints: ['/api/v1/pickups/schedule', '/api/v1/fleet/drivers'],
  },
  {
    id: 'fulfillment',
    name: 'Fulfillment & Hub Transit',
    port: 8083,
    description: 'Cross-dock sorting, barcode scanning, transit container dispatch',
    tech: ['Hub Routing', 'Kafka Stream', 'Liquibase'],
    status: 'UP',
    endpoints: ['/api/v1/hubs/transit', '/api/v1/hubs/scan'],
  },
  {
    id: 'tracking',
    name: 'Realtime GPS & Tracking',
    port: 8084,
    description: 'WebSocket live location broadcasts, geospatial trajectory, Redis cache',
    tech: ['WebSocket / STOMP', 'Redis PubSub', 'Geospatial'],
    status: 'UP',
    endpoints: ['/api/v1/tracking/{code}', '/ws-tracking'],
  },
  {
    id: 'notification',
    name: 'Notification & Alert Dispatcher',
    port: 8085,
    description: 'Multi-channel SMS, Email, Firebase FCM push alerts',
    tech: ['Async Listeners', 'Email/SMS Gateways', 'Kafka'],
    status: 'UP',
    endpoints: ['/api/v1/notifications/send', '/api/v1/notifications/user/{id}'],
  },
];

const SAMPLE_ORDERS = [
  {
    code: 'ORD-984210',
    sender: 'Hà Nội Distribution Hub',
    recipient: 'Đà Nẵng Express Branch',
    weight: '3.4 kg',
    status: 'IN_TRANSIT',
    location: 'Đèo Hải Vân, Đà Nẵng',
    timestamp: '10 phút trước',
    carrier: 'Nguyễn Văn Nam (Fleet-04)',
  },
  {
    code: 'ORD-984211',
    sender: 'Sài Gòn Logistics Center',
    recipient: 'Cần Thơ Hub',
    weight: '12.0 kg',
    status: 'OUT_FOR_DELIVERY',
    location: 'Quận Ninh Kiều, Cần Thơ',
    timestamp: '2 phút trước',
    carrier: 'Lê Hoàng Hải (Shipper-12)',
  },
  {
    code: 'ORD-984212',
    sender: 'Hải Phòng Seaport Depot',
    recipient: 'Bắc Ninh Industrial Park',
    weight: '45.0 kg',
    status: 'PICKUP_SCHEDULED',
    location: 'Kho Tiên Sơn, Bắc Ninh',
    timestamp: '18 phút trước',
    carrier: 'Trần Đình Trọng (Fleet-09)',
  },
];

export default function App() {
  const [activeTab, setActiveTab] = useState<'topology' | 'orders' | 'commands' | 'architecture' | 'threading' | 'devops' | 'postman'>('topology');
  const [copiedFile, setCopiedFile] = useState<string | null>(null);
  const [searchTrackingCode, setSearchTrackingCode] = useState('ORD-984210');
  const [orderList, setOrderList] = useState(SAMPLE_ORDERS);
  const [newOrder, setNewOrder] = useState({
    sender: '',
    recipient: '',
    weight: '1.5',
    type: 'EXPRESS',
    codAmount: '0',
  });
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [creationSuccess, setCreationSuccess] = useState<string | null>(null);

  // Multi-threading Lab State
  const [threadingPricingRunning, setThreadingPricingRunning] = useState(false);
  const [threadingPricingResults, setThreadingPricingResults] = useState<any[] | null>(null);
  const [broadcastRunning, setBroadcastRunning] = useState(false);
  const [broadcastResults, setBroadcastResults] = useState<any[] | null>(null);
  const [driverRankRunning, setDriverRankRunning] = useState(false);
  const [driverRankResults, setDriverRankResults] = useState<any[] | null>(null);

  const runParallelPricingSimulation = () => {
    setThreadingPricingRunning(true);
    setThreadingPricingResults(null);
    setTimeout(() => {
      setThreadingPricingResults([
        { tier: 'STANDARD', baseRate: '22,000 đ', weightSurcharge: '7,500 đ', total: '29,500 đ', thread: 'pricing-async-1', timeMs: 14 },
        { tier: 'EXPRESS', baseRate: '45,000 đ', weightSurcharge: '15,000 đ', total: '60,000 đ', thread: 'pricing-async-2', timeMs: 16 },
        { tier: 'HEAVY_FREIGHT', baseRate: '120,000 đ', weightSurcharge: '38,000 đ', total: '158,000 đ', thread: 'pricing-async-3', timeMs: 19 },
        { tier: 'COLD_CHAIN', baseRate: '180,000 đ', weightSurcharge: '55,000 đ', total: '235,000 đ', thread: 'pricing-async-4', timeMs: 22 },
      ]);
      setThreadingPricingRunning(false);
    }, 600);
  };

  const runParallelBroadcastSimulation = () => {
    setBroadcastRunning(true);
    setBroadcastResults(null);
    setTimeout(() => {
      setBroadcastResults([
        { channel: 'EMAIL', recipient: 'customer@enterprise.com', status: 'SENT', thread: 'notif-worker-1', latency: '42ms' },
        { channel: 'SMS', recipient: '+84988123456', status: 'SENT', thread: 'notif-worker-2', latency: '65ms' },
        { channel: 'ZALO_ZNS', recipient: '0988123456', status: 'SENT', thread: 'notif-worker-3', latency: '38ms' },
        { channel: 'PUSH_FCM', recipient: 'fcm_token_device_99', status: 'SENT', thread: 'notif-worker-4', latency: '29ms' },
      ]);
      setBroadcastRunning(false);
    }, 700);
  };

  const runParallelDriverRanking = () => {
    setDriverRankRunning(true);
    setDriverRankResults(null);
    setTimeout(() => {
      setDriverRankResults([
        { driver: 'Nguyễn Văn Nam (Fleet-04)', distance: '1.2 km', eta: '5 phút', score: 94.0, thread: 'fleet-matcher-1', vehicle: 'MOTORBIKE' },
        { driver: 'Lê Hoàng Hải (Shipper-12)', distance: '2.8 km', eta: '9 phút', score: 86.0, thread: 'fleet-matcher-2', vehicle: 'MOTORBIKE' },
        { driver: 'Trần Đình Trọng (Fleet-09)', distance: '4.5 km', eta: '12 phút', score: 77.5, thread: 'fleet-matcher-3', vehicle: 'VAN_500KG' },
        { driver: 'Võ Minh Quân (Truck-02)', distance: '6.1 km', eta: '16 phút', score: 69.5, thread: 'fleet-matcher-4', vehicle: 'TRUCK_2T' },
      ]);
      setDriverRankRunning(false);
    }, 550);
  };

  const handleCreateOrder = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newOrder.sender || !newOrder.recipient) return;

    setIsSubmitting(true);
    setTimeout(() => {
      const generatedCode = `ORD-${Math.floor(100000 + Math.random() * 900000)}`;
      const created = {
        code: generatedCode,
        sender: newOrder.sender,
        recipient: newOrder.recipient,
        weight: `${newOrder.weight} kg`,
        status: 'ORDER_CREATED',
        location: 'Initial Acceptance Hub',
        timestamp: 'Vừa xong',
        carrier: 'Đang gán tài xế...',
      };
      setOrderList([created, ...orderList]);
      setSearchTrackingCode(generatedCode);
      setCreationSuccess(`Đã tạo đơn hàng #${generatedCode} thành công vào Kafka Topic [order-created-events]!`);
      setIsSubmitting(false);
      setNewOrder({ sender: '', recipient: '', weight: '1.5', type: 'EXPRESS', codAmount: '0' });
    }, 600);
  };

  const trackedOrder = orderList.find((o) => o.code.toUpperCase() === searchTrackingCode.toUpperCase()) || orderList[0];

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col selection:bg-blue-600 selection:text-white">
      {/* Header */}
      <header className="border-b border-slate-800 bg-slate-900/70 backdrop-blur sticky top-0 z-40 px-6 py-4">
        <div className="max-w-7xl mx-auto flex flex-wrap items-center justify-between gap-4">
          <div className="flex items-center space-x-3">
            <div className="h-10 w-10 rounded-xl bg-gradient-to-tr from-blue-600 to-indigo-500 flex items-center justify-center shadow-lg shadow-blue-500/20">
              <Truck className="h-6 w-6 text-white" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h1 className="font-bold text-lg text-white tracking-tight">Logistics Microservices Platform</h1>
                <span className="px-2 py-0.5 text-xs rounded-full bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 font-medium">
                  Spring Boot 3.4 & Java 17
                </span>
              </div>
              <p className="text-xs text-slate-400">Enterprise High-Throughput Delivery & Dispatching Architecture</p>
            </div>
          </div>

          {/* Navigation Tabs */}
          <div className="flex items-center bg-slate-800/80 p-1 rounded-xl border border-slate-700/60">
            <button
              onClick={() => setActiveTab('topology')}
              className={`flex items-center gap-2 px-3.5 py-1.5 rounded-lg text-xs font-medium transition-all ${
                activeTab === 'topology' ? 'bg-blue-600 text-white shadow' : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              <Server className="h-3.5 w-3.5" />
              Service Cluster ({SERVICES.length})
            </button>
            <button
              onClick={() => setActiveTab('orders')}
              className={`flex items-center gap-2 px-3.5 py-1.5 rounded-lg text-xs font-medium transition-all ${
                activeTab === 'orders' ? 'bg-blue-600 text-white shadow' : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              <Package className="h-3.5 w-3.5" />
              Order & Tracking Console
            </button>
            <button
              onClick={() => setActiveTab('commands')}
              className={`flex items-center gap-2 px-3.5 py-1.5 rounded-lg text-xs font-medium transition-all ${
                activeTab === 'commands' ? 'bg-blue-600 text-white shadow' : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              <Terminal className="h-3.5 w-3.5" />
              Docker & Maven Commands
            </button>
            <button
              onClick={() => setActiveTab('architecture')}
              className={`flex items-center gap-2 px-3.5 py-1.5 rounded-lg text-xs font-medium transition-all ${
                activeTab === 'architecture' ? 'bg-blue-600 text-white shadow' : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              <Layers className="h-3.5 w-3.5" />
              Architecture Specs
            </button>
            <button
              onClick={() => setActiveTab('threading')}
              className={`flex items-center gap-2 px-3.5 py-1.5 rounded-lg text-xs font-medium transition-all ${
                activeTab === 'threading' ? 'bg-emerald-600 text-white shadow' : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              <Cpu className="h-3.5 w-3.5" />
              Multi-Threading Engine
            </button>
            <button
              onClick={() => setActiveTab('devops')}
              className={`flex items-center gap-2 px-3.5 py-1.5 rounded-lg text-xs font-medium transition-all ${
                activeTab === 'devops' ? 'bg-cyan-600 text-white shadow' : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              <Cloud className="h-3.5 w-3.5" />
              DevOps, Docker & K8s
            </button>
            <button
              onClick={() => setActiveTab('postman')}
              className={`flex items-center gap-2 px-3.5 py-1.5 rounded-lg text-xs font-medium transition-all ${
                activeTab === 'postman' ? 'bg-indigo-600 text-white shadow' : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              <FileCode className="h-3.5 w-3.5" />
              Postman Collection
            </button>
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="flex-1 max-w-7xl w-full mx-auto p-6 space-y-6">
        {/* Quick Metrics Bar */}
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
          <div className="bg-slate-900/60 border border-slate-800 rounded-xl p-4 flex items-center justify-between">
            <div>
              <p className="text-xs text-slate-400 font-medium">Active Services</p>
              <p className="text-2xl font-bold text-white mt-1">8 / 8</p>
            </div>
            <div className="h-10 w-10 rounded-lg bg-emerald-500/10 border border-emerald-500/20 flex items-center justify-center text-emerald-400">
              <CheckCircle2 className="h-5 w-5" />
            </div>
          </div>
          <div className="bg-slate-900/60 border border-slate-800 rounded-xl p-4 flex items-center justify-between">
            <div>
              <p className="text-xs text-slate-400 font-medium">Gateway Port</p>
              <p className="text-2xl font-bold text-blue-400 mt-1">:8000</p>
            </div>
            <div className="h-10 w-10 rounded-lg bg-blue-500/10 border border-blue-500/20 flex items-center justify-center text-blue-400">
              <Radio className="h-5 w-5" />
            </div>
          </div>
          <div className="bg-slate-900/60 border border-slate-800 rounded-xl p-4 flex items-center justify-between">
            <div>
              <p className="text-xs text-slate-400 font-medium">Message Broker</p>
              <p className="text-2xl font-bold text-indigo-400 mt-1">Kafka 9092</p>
            </div>
            <div className="h-10 w-10 rounded-lg bg-indigo-500/10 border border-indigo-500/20 flex items-center justify-center text-indigo-400">
              <Cpu className="h-5 w-5" />
            </div>
          </div>
          <div className="bg-slate-900/60 border border-slate-800 rounded-xl p-4 flex items-center justify-between">
            <div>
              <p className="text-xs text-slate-400 font-medium">Distributed Store</p>
              <p className="text-2xl font-bold text-amber-400 mt-1">Postgres + Redis</p>
            </div>
            <div className="h-10 w-10 rounded-lg bg-amber-500/10 border border-amber-500/20 flex items-center justify-center text-amber-400">
              <Database className="h-5 w-5" />
            </div>
          </div>
        </div>

        {/* TAB 1: CLUSTER TOPOLOGY */}
        {activeTab === 'topology' && (
          <div className="space-y-6">
            <div className="flex items-center justify-between">
              <div>
                <h2 className="text-lg font-semibold text-white">Microservices Cluster Topology</h2>
                <p className="text-xs text-slate-400">Tất cả các dịch vụ độc lập cấu hình Java 17 + Spring Boot 3.4.2</p>
              </div>
              <span className="text-xs px-2.5 py-1 rounded bg-slate-800 text-slate-300 border border-slate-700 flex items-center gap-1.5">
                <RefreshCw className="h-3 w-3 animate-spin text-blue-400" /> Eureka Heartbeat: Healthy
              </span>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
              {SERVICES.map((svc) => (
                <div
                  key={svc.id}
                  className="bg-slate-900/80 border border-slate-800 hover:border-slate-700 rounded-xl p-4 flex flex-col justify-between transition-all hover:shadow-lg hover:shadow-black/40"
                >
                  <div>
                    <div className="flex items-start justify-between gap-2">
                      <h3 className="font-semibold text-sm text-white">{svc.name}</h3>
                      <span className="px-2 py-0.5 rounded text-[11px] font-bold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                        :{svc.port}
                      </span>
                    </div>
                    <p className="text-xs text-slate-400 mt-2 line-clamp-2 leading-relaxed">{svc.description}</p>
                  </div>

                  <div className="mt-4 pt-3 border-t border-slate-800/80 space-y-2">
                    <div className="flex flex-wrap gap-1">
                      {svc.tech.map((t, idx) => (
                        <span key={idx} className="px-1.5 py-0.5 text-[10px] bg-slate-800 text-slate-300 rounded">
                          {t}
                        </span>
                      ))}
                    </div>
                    <div className="text-[11px] font-mono text-blue-400 bg-slate-950/60 p-1.5 rounded border border-slate-800/60 truncate">
                      {svc.endpoints[0]}
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* TAB 2: ORDER & TRACKING CONSOLE */}
        {activeTab === 'orders' && (
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
            {/* Create Order Simulator */}
            <div className="lg:col-span-5 bg-slate-900/80 border border-slate-800 rounded-xl p-5 space-y-4">
              <div className="flex items-center gap-2">
                <PlusCircle className="h-5 w-5 text-blue-400" />
                <h3 className="font-semibold text-white">Tạo Đơn Hàng Mới (Simulate Order API)</h3>
              </div>
              <p className="text-xs text-slate-400">
                Gửi lệnh tạo đơn qua API Gateway <code className="text-blue-400">POST /api/v1/orders</code> với Outbox Event.
              </p>

              {creationSuccess && (
                <div className="p-3 bg-emerald-950/50 border border-emerald-800/60 rounded-lg text-emerald-300 text-xs flex items-start gap-2">
                  <CheckCircle2 className="h-4 w-4 mt-0.5 text-emerald-400 shrink-0" />
                  <span>{creationSuccess}</span>
                </div>
              )}

              <form onSubmit={handleCreateOrder} className="space-y-3">
                <div>
                  <label className="block text-xs font-medium text-slate-300 mb-1">Địa Chỉ Gửi (Sender)</label>
                  <input
                    type="text"
                    required
                    placeholder="VD: Kho Cầu Giấy, Hà Nội"
                    value={newOrder.sender}
                    onChange={(e) => setNewOrder({ ...newOrder, sender: e.target.value })}
                    className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-xs text-slate-100 placeholder-slate-500 focus:outline-none focus:border-blue-500"
                  />
                </div>

                <div>
                  <label className="block text-xs font-medium text-slate-300 mb-1">Địa Chỉ Nhận (Recipient)</label>
                  <input
                    type="text"
                    required
                    placeholder="VD: 120 Nguyễn Văn Linh, Đà Nẵng"
                    value={newOrder.recipient}
                    onChange={(e) => setNewOrder({ ...newOrder, recipient: e.target.value })}
                    className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-xs text-slate-100 placeholder-slate-500 focus:outline-none focus:border-blue-500"
                  />
                </div>

                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className="block text-xs font-medium text-slate-300 mb-1">Trọng Lượng (kg)</label>
                    <input
                      type="number"
                      step="0.1"
                      required
                      value={newOrder.weight}
                      onChange={(e) => setNewOrder({ ...newOrder, weight: e.target.value })}
                      className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-xs text-slate-100 focus:outline-none focus:border-blue-500"
                    />
                  </div>
                  <div>
                    <label className="block text-xs font-medium text-slate-300 mb-1">Loại Dịch Vụ</label>
                    <select
                      value={newOrder.type}
                      onChange={(e) => setNewOrder({ ...newOrder, type: e.target.value })}
                      className="w-full bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-xs text-slate-100 focus:outline-none focus:border-blue-500"
                    >
                      <option value="EXPRESS">Hỏa Tốc (Express)</option>
                      <option value="STANDARD">Tiêu Chuẩn (Standard)</option>
                      <option value="SAMEDAY">Trong Ngày (SameDay)</option>
                    </select>
                  </div>
                </div>

                <button
                  type="submit"
                  disabled={isSubmitting}
                  className="w-full py-2.5 px-4 bg-blue-600 hover:bg-blue-500 disabled:opacity-50 text-white rounded-lg text-xs font-semibold flex items-center justify-center gap-2 transition-all shadow-md shadow-blue-600/20"
                >
                  {isSubmitting ? (
                    <>
                      <RefreshCw className="h-3.5 w-3.5 animate-spin" /> Đang phát Outbox Event...
                    </>
                  ) : (
                    <>
                      <Play className="h-3.5 w-3.5" /> Tạo Đơn & Phát Event Kafka
                    </>
                  )}
                </button>
              </form>
            </div>

            {/* Tracking & Lifecycle Timeline */}
            <div className="lg:col-span-7 space-y-4">
              <div className="bg-slate-900/80 border border-slate-800 rounded-xl p-5 space-y-4">
                <div className="flex items-center justify-between gap-3">
                  <div className="flex items-center gap-2">
                    <MapPin className="h-5 w-5 text-indigo-400" />
                    <h3 className="font-semibold text-white">Tra Cứu Tiến Trình Vận Chuyển (Realtime Tracking)</h3>
                  </div>
                  <div className="relative">
                    <input
                      type="text"
                      value={searchTrackingCode}
                      onChange={(e) => setSearchTrackingCode(e.target.value)}
                      placeholder="Mã đơn hàng..."
                      className="bg-slate-950 border border-slate-800 rounded-lg pl-3 pr-8 py-1.5 text-xs text-slate-200 focus:outline-none focus:border-indigo-500"
                    />
                    <Search className="h-3.5 w-3.5 text-slate-500 absolute right-2.5 top-2.5" />
                  </div>
                </div>

                {/* Tracking Details Card */}
                {trackedOrder ? (
                  <div className="bg-slate-950 border border-slate-800 rounded-xl p-4 space-y-4">
                    <div className="flex flex-wrap items-center justify-between gap-2 border-b border-slate-800/80 pb-3">
                      <div>
                        <span className="text-xs font-bold text-indigo-400 tracking-wider">
                          MÃ VẬN ĐƠN: {trackedOrder.code}
                        </span>
                        <p className="text-xs text-slate-400 mt-0.5">Khối lượng: {trackedOrder.weight} • Tài xế: {trackedOrder.carrier}</p>
                      </div>
                      <span className="px-2.5 py-1 rounded-full text-xs font-bold bg-blue-500/10 text-blue-400 border border-blue-500/20">
                        {trackedOrder.status}
                      </span>
                    </div>

                    {/* Timeline */}
                    <div className="space-y-3 relative before:absolute before:inset-0 before:left-3.5 before:w-0.5 before:bg-slate-800">
                      <div className="flex items-start gap-3 relative z-10">
                        <div className="h-7 w-7 rounded-full bg-emerald-500/20 border border-emerald-500/40 text-emerald-400 flex items-center justify-center shrink-0">
                          <CheckCircle2 className="h-4 w-4" />
                        </div>
                        <div>
                          <p className="text-xs font-medium text-slate-200">Tiếp nhận đơn hàng (Order Created)</p>
                          <p className="text-[11px] text-slate-400">{trackedOrder.sender}</p>
                        </div>
                      </div>

                      <div className="flex items-start gap-3 relative z-10">
                        <div className="h-7 w-7 rounded-full bg-blue-500/20 border border-blue-500/40 text-blue-400 flex items-center justify-center shrink-0">
                          <Truck className="h-4 w-4" />
                        </div>
                        <div>
                          <p className="text-xs font-medium text-slate-200">Đang luân chuyển / Hub Transit</p>
                          <p className="text-[11px] text-slate-400">Vị trí hiện tại: {trackedOrder.location} ({trackedOrder.timestamp})</p>
                        </div>
                      </div>

                      <div className="flex items-start gap-3 relative z-10">
                        <div className="h-7 w-7 rounded-full bg-slate-800 border border-slate-700 text-slate-500 flex items-center justify-center shrink-0">
                          <MapPin className="h-4 w-4" />
                        </div>
                        <div>
                          <p className="text-xs font-medium text-slate-400">Điểm giao hàng đích</p>
                          <p className="text-[11px] text-slate-500">{trackedOrder.recipient}</p>
                        </div>
                      </div>
                    </div>
                  </div>
                ) : (
                  <p className="text-xs text-slate-500">Không tìm thấy mã đơn hàng phù hợp.</p>
                )}
              </div>
            </div>
          </div>
        )}

        {/* TAB 3: COMMANDS */}
        {activeTab === 'commands' && (
          <div className="space-y-4 bg-slate-900/80 border border-slate-800 rounded-xl p-5">
            <div className="flex items-center gap-2">
              <Terminal className="h-5 w-5 text-emerald-400" />
              <h3 className="font-semibold text-white">Lệnh Vận Hành & Khởi Động Backend</h3>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div className="space-y-2">
                <p className="text-xs font-medium text-slate-300">1. Biên dịch tất cả Microservices bằng Maven (Java 17 LTS)</p>
                <div className="bg-slate-950 p-3 rounded-lg border border-slate-800 font-mono text-xs text-emerald-400">
                  cd microservices<br />
                  mvn clean package -DskipTests
                </div>
              </div>

              <div className="space-y-2">
                <p className="text-xs font-medium text-slate-300">2. Khởi chạy toàn bộ hệ thống bằng Docker Compose</p>
                <div className="bg-slate-950 p-3 rounded-lg border border-slate-800 font-mono text-xs text-blue-400">
                  docker-compose up --build -d
                </div>
              </div>

              <div className="space-y-2">
                <p className="text-xs font-medium text-slate-300">3. Xem Logs của Service cụ thể</p>
                <div className="bg-slate-950 p-3 rounded-lg border border-slate-800 font-mono text-xs text-slate-300">
                  docker-compose logs -f order-service<br />
                  docker-compose logs -f api-gateway
                </div>
              </div>

              <div className="space-y-2">
                <p className="text-xs font-medium text-slate-300">4. Tắt hoặc Reset toàn bộ dữ liệu</p>
                <div className="bg-slate-950 p-3 rounded-lg border border-slate-800 font-mono text-xs text-rose-400">
                  docker-compose down -v
                </div>
              </div>
            </div>
          </div>
        )}

        {/* TAB 4: ARCHITECTURE SPECS */}
        {activeTab === 'architecture' && (
          <div className="bg-slate-900/80 border border-slate-800 rounded-xl p-5 space-y-4">
            <h3 className="font-semibold text-white">Kiến Trúc Kỹ Thuật Hệ Thống (Enterprise Patterns)</h3>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4 text-xs">
              <div className="bg-slate-950 p-4 rounded-xl border border-slate-800 space-y-2">
                <p className="font-semibold text-blue-400">Transactional Outbox Pattern</p>
                <p className="text-slate-400 leading-relaxed">
                  Đảm bảo tính toàn vẹn 100% giữa Database transaction và Kafka Event publishing mà không bị mất mát message khi có sự cố mạng.
                </p>
              </div>
              <div className="bg-slate-950 p-4 rounded-xl border border-slate-800 space-y-2">
                <p className="font-semibold text-indigo-400">Distributed Lock (Redisson)</p>
                <p className="text-slate-400 leading-relaxed">
                  Tránh xung đột tài nguyên khi hàng nghìn tài xế cùng tranh chấp hoặc nhận đơn hàng trong cùng một giây.
                </p>
              </div>
              <div className="bg-slate-950 p-4 rounded-xl border border-slate-800 space-y-2">
                <p className="font-semibold text-emerald-400">Realtime STOMP WebSocket</p>
                <p className="text-slate-400 leading-relaxed">
                  Truyền tọa độ GPS trực tiếp tới khách hàng và quản lý vận hành với độ trễ dưới 50ms qua kênh WebSocket kết nối Redis.
                </p>
              </div>
            </div>
          </div>
        )}

        {/* TAB: MULTI-THREADING ENGINE */}
        {activeTab === 'threading' && (
          <div className="space-y-6">
            {/* Header / Intro */}
            <div className="bg-slate-900/80 border border-slate-800 rounded-xl p-5 space-y-4">
              <div className="flex flex-wrap items-center justify-between gap-3">
                <div className="flex items-center gap-2.5">
                  <div className="h-9 w-9 rounded-lg bg-emerald-500/10 border border-emerald-500/20 flex items-center justify-center text-emerald-400">
                    <Cpu className="h-5 w-5" />
                  </div>
                  <div>
                    <h3 className="font-semibold text-white">Multi-Threading & Concurrent Async Worker Engine</h3>
                    <p className="text-xs text-slate-400">
                      Cấu hình ThreadPoolTaskExecutor, CompletableFuture, Barrier Synchronization & Parallel Dispatching trong Spring Boot
                    </p>
                  </div>
                </div>
                <span className="px-2.5 py-1 text-xs rounded bg-emerald-950/60 border border-emerald-800 text-emerald-300 font-mono">
                  ThreadPoolExecutor: RUNNING
                </span>
              </div>

              {/* Thread Pools Overview */}
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3">
                <div className="bg-slate-950 p-3.5 rounded-lg border border-slate-800/80 space-y-1">
                  <div className="flex items-center justify-between">
                    <span className="text-[11px] font-semibold text-blue-400">outboxTaskExecutor</span>
                    <span className="text-[10px] font-mono text-slate-400">order-service</span>
                  </div>
                  <p className="text-xs text-slate-300 font-mono">Core: 5 | Max: 15</p>
                  <p className="text-[11px] text-slate-500">Queue: 200 • CallerRunsPolicy</p>
                </div>
                <div className="bg-slate-950 p-3.5 rounded-lg border border-slate-800/80 space-y-1">
                  <div className="flex items-center justify-between">
                    <span className="text-[11px] font-semibold text-emerald-400">pricingTaskExecutor</span>
                    <span className="text-[10px] font-mono text-slate-400">order-service</span>
                  </div>
                  <p className="text-xs text-slate-300 font-mono">Core: 4 | Max: 10</p>
                  <p className="text-[11px] text-slate-500">Parallel 4 Tiers • CompletableFuture</p>
                </div>
                <div className="bg-slate-950 p-3.5 rounded-lg border border-slate-800/80 space-y-1">
                  <div className="flex items-center justify-between">
                    <span className="text-[11px] font-semibold text-amber-400">notificationTaskExecutor</span>
                    <span className="text-[10px] font-mono text-slate-400">notification-service</span>
                  </div>
                  <p className="text-xs text-slate-300 font-mono">Core: 10 | Max: 30</p>
                  <p className="text-[11px] text-slate-500">Queue: 500 • Multi-Channel Broadcast</p>
                </div>
                <div className="bg-slate-950 p-3.5 rounded-lg border border-slate-800/80 space-y-1">
                  <div className="flex items-center justify-between">
                    <span className="text-[11px] font-semibold text-indigo-400">fleetMatchingExecutor</span>
                    <span className="text-[10px] font-mono text-slate-400">pickup-fleet-service</span>
                  </div>
                  <p className="text-xs text-slate-300 font-mono">Core: 6 | Max: 20</p>
                  <p className="text-[11px] text-slate-500">Haversine Spatial Ranking Worker</p>
                </div>
              </div>
            </div>

            {/* Interactive Multi-Threading Scenarios */}
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
              {/* Scenario 1: Parallel 4-Tier Pricing Aggregator */}
              <div className="bg-slate-900/80 border border-slate-800 rounded-xl p-5 space-y-4 flex flex-col justify-between">
                <div className="space-y-2">
                  <div className="flex items-center gap-2">
                    <span className="h-6 w-6 rounded bg-emerald-500/10 text-emerald-400 font-mono text-xs flex items-center justify-center font-bold">1</span>
                    <h4 className="font-semibold text-white text-sm">Parallel Pricing Aggregator</h4>
                  </div>
                  <p className="text-xs text-slate-400 leading-relaxed">
                    Khởi tạo 4 Worker Threads đồng thời để tính toán cước phí cho 4 dịch vụ (Standard, Express, Freight, Cold Chain) và tổng hợp kết quả bằng <code className="text-emerald-400">CompletableFuture.allOf()</code>.
                  </p>
                </div>

                <div className="space-y-3">
                  {threadingPricingResults && (
                    <div className="space-y-1.5 bg-slate-950 p-3 rounded-lg border border-slate-800 text-[11px]">
                      {threadingPricingResults.map((r, i) => (
                        <div key={i} className="flex items-center justify-between border-b border-slate-800/60 pb-1 last:border-0 last:pb-0">
                          <span className="font-semibold text-slate-200">{r.tier}</span>
                          <span className="font-mono text-emerald-400">{r.total}</span>
                          <span className="text-[10px] font-mono text-slate-500">[{r.thread} • {r.timeMs}ms]</span>
                        </div>
                      ))}
                    </div>
                  )}

                  <button
                    onClick={runParallelPricingSimulation}
                    disabled={threadingPricingRunning}
                    className="w-full py-2 px-3 bg-emerald-600 hover:bg-emerald-500 disabled:opacity-50 text-white rounded-lg text-xs font-semibold flex items-center justify-center gap-2 transition-all"
                  >
                    {threadingPricingRunning ? (
                      <>
                        <RefreshCw className="h-3.5 w-3.5 animate-spin" /> Đang chạy 4 Threads song song...
                      </>
                    ) : (
                      <>
                        <Play className="h-3.5 w-3.5" /> Chạy Parallel Pricing (POST /calculate-tiers-concurrently)
                      </>
                    )}
                  </button>
                </div>
              </div>

              {/* Scenario 2: Multi-Channel Broadcast */}
              <div className="bg-slate-900/80 border border-slate-800 rounded-xl p-5 space-y-4 flex flex-col justify-between">
                <div className="space-y-2">
                  <div className="flex items-center gap-2">
                    <span className="h-6 w-6 rounded bg-amber-500/10 text-amber-400 font-mono text-xs flex items-center justify-center font-bold">2</span>
                    <h4 className="font-semibold text-white text-sm">Concurrent Multi-Channel Alert</h4>
                  </div>
                  <p className="text-xs text-slate-400 leading-relaxed">
                    Phát tán thông báo khẩn cấp tới SMS, Email, Zalo ZNS và FCM Push đồng thời trên các luồng độc lập trong Thread Pool thay vì gửi tuần tự.
                  </p>
                </div>

                <div className="space-y-3">
                  {broadcastResults && (
                    <div className="space-y-1.5 bg-slate-950 p-3 rounded-lg border border-slate-800 text-[11px]">
                      {broadcastResults.map((b, i) => (
                        <div key={i} className="flex items-center justify-between border-b border-slate-800/60 pb-1 last:border-0 last:pb-0">
                          <span className="font-semibold text-amber-300">{b.channel}</span>
                          <span className="text-emerald-400 font-mono text-[10px]">{b.status}</span>
                          <span className="text-[10px] font-mono text-slate-500">[{b.thread} • {b.latency}]</span>
                        </div>
                      ))}
                    </div>
                  )}

                  <button
                    onClick={runParallelBroadcastSimulation}
                    disabled={broadcastRunning}
                    className="w-full py-2 px-3 bg-amber-600 hover:bg-amber-500 disabled:opacity-50 text-white rounded-lg text-xs font-semibold flex items-center justify-center gap-2 transition-all"
                  >
                    {broadcastRunning ? (
                      <>
                        <RefreshCw className="h-3.5 w-3.5 animate-spin" /> Đang broadcast 4 kênh đồng thời...
                      </>
                    ) : (
                      <>
                        <Play className="h-3.5 w-3.5" /> Chạy Parallel Broadcast (POST /broadcast-parallel)
                      </>
                    )}
                  </button>
                </div>
              </div>

              {/* Scenario 3: Geo-Spatial Driver Ranking */}
              <div className="bg-slate-900/80 border border-slate-800 rounded-xl p-5 space-y-4 flex flex-col justify-between">
                <div className="space-y-2">
                  <div className="flex items-center gap-2">
                    <span className="h-6 w-6 rounded bg-indigo-500/10 text-indigo-400 font-mono text-xs flex items-center justify-center font-bold">3</span>
                    <h4 className="font-semibold text-white text-sm">Parallel Fleet Spatial Ranking</h4>
                  </div>
                  <p className="text-xs text-slate-400 leading-relaxed">
                    Tính toán khoảng cách Haversine và ước lượng ETA cho toàn bộ danh sách tài xế khả dụng song song trên worker threads để tìm tài xế tối ưu nhất.
                  </p>
                </div>

                <div className="space-y-3">
                  {driverRankResults && (
                    <div className="space-y-1.5 bg-slate-950 p-3 rounded-lg border border-slate-800 text-[11px]">
                      {driverRankResults.map((d, i) => (
                        <div key={i} className="flex items-center justify-between border-b border-slate-800/60 pb-1 last:border-0 last:pb-0">
                          <span className="font-semibold text-slate-200 truncate max-w-[120px]">{d.driver}</span>
                          <span className="font-mono text-indigo-400">{d.distance} ({d.eta})</span>
                          <span className="text-[10px] font-mono text-slate-500">Score: {d.score}</span>
                        </div>
                      ))}
                    </div>
                  )}

                  <button
                    onClick={runParallelDriverRanking}
                    disabled={driverRankRunning}
                    className="w-full py-2 px-3 bg-indigo-600 hover:bg-indigo-500 disabled:opacity-50 text-white rounded-lg text-xs font-semibold flex items-center justify-center gap-2 transition-all"
                  >
                    {driverRankRunning ? (
                      <>
                        <RefreshCw className="h-3.5 w-3.5 animate-spin" /> Đang xếp hạng tài xế song song...
                      </>
                    ) : (
                      <>
                        <Play className="h-3.5 w-3.5" /> Chạy Spatial Ranker (GET /rank-drivers-concurrently)
                      </>
                    )}
                  </button>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* TAB: DEVOPS, DOCKER & KUBERNETES LAB */}
        {activeTab === 'devops' && <DevOpsLab />}

        {/* TAB 5: POSTMAN COLLECTION */}
        {activeTab === 'postman' && (
          <div className="space-y-6">
            <div className="bg-slate-900/80 border border-slate-800 rounded-xl p-5 space-y-4">
              <div className="flex flex-wrap items-center justify-between gap-3">
                <div className="flex items-center gap-2.5">
                  <div className="h-9 w-9 rounded-lg bg-orange-500/10 border border-orange-500/20 flex items-center justify-center text-orange-400">
                    <FileCode className="h-5 w-5" />
                  </div>
                  <div>
                    <h3 className="font-semibold text-white">Postman Collection & Environment V2.1</h3>
                    <p className="text-xs text-slate-400">Tải về hoặc copy để Import vào Postman kiểm thử toàn bộ API qua Spring Cloud Gateway</p>
                  </div>
                </div>

                <div className="flex items-center gap-2">
                  <a
                    href="/postman_collection.json"
                    download="postman_collection.json"
                    className="px-3 py-1.5 bg-indigo-600 hover:bg-indigo-500 text-white rounded-lg text-xs font-medium flex items-center gap-1.5 transition-all shadow"
                  >
                    <Download className="h-3.5 w-3.5" /> Tải postman_collection.json
                  </a>
                  <a
                    href="/postman_environment.json"
                    download="postman_environment.json"
                    className="px-3 py-1.5 bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-700 rounded-lg text-xs font-medium flex items-center gap-1.5 transition-all"
                  >
                    <Download className="h-3.5 w-3.5" /> Tải postman_environment.json
                  </a>
                </div>
              </div>

              {/* Instructions */}
              <div className="p-4 bg-slate-950 border border-slate-800/90 rounded-lg text-xs space-y-2">
                <p className="font-semibold text-blue-400">📖 Các bước Import vào Postman:</p>
                <ol className="list-decimal list-inside space-y-1 text-slate-300">
                  <li>Mở ứng dụng <strong>Postman</strong>.</li>
                  <li>Bấm nút <strong>Import</strong> ở góc trên bên trái.</li>
                  <li>Kéo thả hoặc chọn 2 file: <code>postman_collection.json</code> và <code>postman_environment.json</code>.</li>
                  <li>Chọn Environment <strong>"Logistics Local Development Environment"</strong> ở góc trên bên phải của Postman.</li>
                  <li>Chạy request <strong>1.1. Login</strong> để Postman tự động lưu JWT token vào biến <code>jwt_token</code>.</li>
                </ol>
              </div>

              {/* Endpoint Overview Table */}
              <div className="space-y-3">
                <h4 className="text-xs font-semibold text-slate-300">Danh Sách 16 Endpoints Đã Cấu Hình:</h4>
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">
                  <div className="bg-slate-950 p-3 rounded-lg border border-slate-800/80 space-y-1.5">
                    <span className="text-[11px] font-bold text-amber-400">1. Authentication IAM (:8080)</span>
                    <p className="text-[11px] text-slate-400 font-mono">POST /api/v1/auth/login</p>
                    <p className="text-[11px] text-slate-400 font-mono">POST /api/v1/auth/register</p>
                  </div>
                  <div className="bg-slate-950 p-3 rounded-lg border border-slate-800/80 space-y-1.5">
                    <span className="text-[11px] font-bold text-blue-400">2. Order Management (:8081)</span>
                    <p className="text-[11px] text-slate-400 font-mono">POST /api/v1/orders (Outbox)</p>
                    <p className="text-[11px] text-slate-400 font-mono">POST /api/v1/orders/calculate-price</p>
                    <p className="text-[11px] text-slate-400 font-mono">GET /api/v1/orders/track/{'{code}'}</p>
                    <p className="text-[11px] text-slate-400 font-mono">PUT /api/v1/orders/{'{id}'}/status</p>
                  </div>
                  <div className="bg-slate-950 p-3 rounded-lg border border-slate-800/80 space-y-1.5">
                    <span className="text-[11px] font-bold text-emerald-400">3. Pickup & Fleet (:8082)</span>
                    <p className="text-[11px] text-slate-400 font-mono">GET /api/v1/fleet/drivers</p>
                    <p className="text-[11px] text-slate-400 font-mono">POST /api/v1/fleet/drivers</p>
                    <p className="text-[11px] text-slate-400 font-mono">GET /api/v1/fleet/find-nearest-driver</p>
                    <p className="text-[11px] text-slate-400 font-mono">POST /api/v1/fleet/pickups/assign</p>
                  </div>
                  <div className="bg-slate-950 p-3 rounded-lg border border-slate-800/80 space-y-1.5">
                    <span className="text-[11px] font-bold text-purple-400">4. Fulfillment & Hub (:8083)</span>
                    <p className="text-[11px] text-slate-400 font-mono">POST /api/v1/fulfillment/hub-transit/scan</p>
                    <p className="text-[11px] text-slate-400 font-mono">GET /api/v1/fulfillment/hub-transit/{'{code}'}</p>
                    <p className="text-[11px] text-slate-400 font-mono">POST /api/v1/fulfillment/pod (Signature)</p>
                    <p className="text-[11px] text-slate-400 font-mono">GET /api/v1/fulfillment/pod/{'{code}'}</p>
                  </div>
                  <div className="bg-slate-950 p-3 rounded-lg border border-slate-800/80 space-y-1.5">
                    <span className="text-[11px] font-bold text-cyan-400">5. GPS Telemetry (:8084)</span>
                    <p className="text-[11px] text-slate-400 font-mono">POST /api/v1/tracking/events</p>
                    <p className="text-[11px] text-slate-400 font-mono">GET /api/v1/tracking/{'{code}'}</p>
                  </div>
                  <div className="bg-slate-950 p-3 rounded-lg border border-slate-800/80 space-y-1.5">
                    <span className="text-[11px] font-bold text-rose-400">6. Notifications (:8085)</span>
                    <p className="text-[11px] text-slate-400 font-mono">POST /api/v1/notifications/send-manual</p>
                    <p className="text-[11px] text-slate-400 font-mono">GET /api/v1/notifications/logs</p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        )}
      </main>

      {/* Footer */}
      <footer className="border-t border-slate-800/80 py-4 px-6 text-center text-xs text-slate-500">
        Logistics Enterprise Microservices Platform • Spring Boot 3.4.2 • Java 17 LTS • Docker & Kubernetes Ready
      </footer>
    </div>
  );
}
