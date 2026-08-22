import React, { useState, useEffect } from "react";
import {
  Server,
  Layers,
  Database,
  Cpu,
  Activity,
  Terminal,
  FileCode2,
  CheckCircle2,
  AlertCircle,
  Play,
  Send,
  Radio,
  Boxes,
  Compass,
  FileText,
  ShieldCheck,
  RefreshCw,
  Copy,
  ExternalLink,
  ChevronRight,
  Code
} from "lucide-react";

interface MicroserviceInfo {
  name: string;
  port: number;
  path: string;
  storage: string;
  events?: string[];
  features: string[];
}

interface ArchitectureData {
  platformName: string;
  runtime: string;
  frameworks: string[];
  microservices: MicroserviceInfo[];
  outboxEventsCount: number;
  activeOrdersCount: number;
  activeDriversCount: number;
}

interface JavaFile {
  relativePath: string;
  size: number;
  content: string;
}

export default function App() {
  const [arch, setArch] = useState<ArchitectureData | null>(null);
  const [javaFiles, setJavaFiles] = useState<JavaFile[]>([]);
  const [selectedFile, setSelectedFile] = useState<JavaFile | null>(null);
  const [activeTab, setActiveTab] = useState<"services" | "api" | "outbox" | "source" | "deploy">("services");
  const [healthStatus, setHealthStatus] = useState<any>(null);
  const [loading, setLoading] = useState(false);

  // API Tester State
  const [testEndpoint, setTestEndpoint] = useState("/api/v1/orders/calculate-price");
  const [testMethod, setTestMethod] = useState("POST");
  const [testBody, setTestBody] = useState(JSON.stringify({
    weightKg: 3.5,
    distanceKm: 12.0,
    declaredValue: 2500000,
    codAmount: 850000,
    expressDelivery: true
  }, null, 2));
  const [apiResponse, setApiResponse] = useState<string>("// Run an endpoint query to see live JSON response");

  // Outbox events
  const [outboxList, setOutboxList] = useState<any[]>([]);

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    setLoading(true);
    try {
      const archRes = await fetch("/api/v1/microservices/architecture");
      if (archRes.ok) {
        const data = await archRes.json();
        setArch(data);
      }

      const filesRes = await fetch("/api/v1/microservices/files");
      if (filesRes.ok) {
        const filesData = await filesRes.json();
        setJavaFiles(filesData.files || []);
        if (filesData.files?.length > 0 && !selectedFile) {
          setSelectedFile(filesData.files[0]);
        }
      }

      const healthRes = await fetch("/actuator/health");
      if (healthRes.ok) {
        setHealthStatus(await healthRes.json());
      }
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  const runApiTest = async () => {
    try {
      setApiResponse("Executing request...");
      const options: RequestInit = {
        method: testMethod,
        headers: { "Content-Type": "application/json" }
      };
      if (testMethod === "POST" || testMethod === "PUT") {
        options.body = testBody;
      }
      const res = await fetch(testEndpoint, options);
      const data = await res.json();
      setApiResponse(JSON.stringify(data, null, 2));
    } catch (err: any) {
      setApiResponse(JSON.stringify({ error: err.message }, null, 2));
    }
  };

  const predefinedTests = [
    {
      label: "Estimate Dynamic Shipping Price",
      method: "POST",
      url: "/api/v1/orders/calculate-price",
      body: { weightKg: 3.5, distanceKm: 12.0, declaredValue: 2500000, codAmount: 850000, expressDelivery: true }
    },
    {
      label: "Create Shipment Order (Transactional Outbox)",
      method: "POST",
      url: "/api/v1/orders",
      body: {
        senderName: "TechVN Logistics",
        senderPhone: "0901234567",
        senderAddress: "72 Lê Thánh Tôn, Bến Nghé, Quận 1, TP.HCM",
        recipientName: "Phạm Thu Hằng",
        recipientPhone: "0988776655",
        recipientAddress: "123 Hoàng Hoa Thám, Tân Bình, TP.HCM",
        totalWeightKg: 2.5,
        declaredValue: 1200000,
        codAmount: 500000,
        specialInstructions: "Giao giờ hành chính"
      }
    },
    {
      label: "List Active Courier Drivers",
      method: "GET",
      url: "/api/v1/fleet/drivers",
      body: null
    },
    {
      label: "Query Shipment Timeline Tracking",
      method: "GET",
      url: "/api/v1/tracking/VNX99281720",
      body: null
    },
    {
      label: "Spring Boot Actuator Health Probe",
      method: "GET",
      url: "/actuator/health",
      body: null
    }
  ];

  return (
    <div id="enterprise-backend-console" className="min-h-screen bg-slate-950 text-slate-100 font-mono antialiased flex flex-col">
      {/* Top Bar */}
      <header className="border-b border-slate-800 bg-slate-900/90 backdrop-blur px-6 py-3.5 flex items-center justify-between sticky top-0 z-30">
        <div className="flex items-center space-x-3">
          <div className="w-8 h-8 rounded-lg bg-emerald-500/10 border border-emerald-500/30 flex items-center justify-center text-emerald-400">
            <Server className="w-4 h-4" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <span className="font-bold text-sm text-white tracking-wide">
                Logistics &amp; Delivery Microservices Platform
              </span>
              <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded text-[11px] font-semibold bg-emerald-500/20 text-emerald-400 border border-emerald-500/30">
                <CheckCircle2 className="w-3 h-3" /> Spring Boot 3.4.2 &bull; Java 21
              </span>
            </div>
            <p className="text-xs text-slate-400 font-sans">
              Distributed Event-Driven Architecture with Kafka Transactional Outbox, Redis Geo, and Service Mesh
            </p>
          </div>
        </div>

        {/* Navigation Tabs */}
        <div className="flex items-center bg-slate-950 p-1 rounded-lg border border-slate-800 space-x-1 text-xs">
          <button
            onClick={() => setActiveTab("services")}
            className={`px-3 py-1.5 rounded-md transition-all flex items-center gap-1.5 ${
              activeTab === "services" ? "bg-emerald-600 text-white shadow-sm font-semibold" : "text-slate-400 hover:text-slate-200"
            }`}
          >
            <Boxes className="w-3.5 h-3.5" /> Microservices Mesh
          </button>
          <button
            onClick={() => setActiveTab("api")}
            className={`px-3 py-1.5 rounded-md transition-all flex items-center gap-1.5 ${
              activeTab === "api" ? "bg-emerald-600 text-white shadow-sm font-semibold" : "text-slate-400 hover:text-slate-200"
            }`}
          >
            <Terminal className="w-3.5 h-3.5" /> Live REST API Tester
          </button>
          <button
            onClick={() => setActiveTab("source")}
            className={`px-3 py-1.5 rounded-md transition-all flex items-center gap-1.5 ${
              activeTab === "source" ? "bg-emerald-600 text-white shadow-sm font-semibold" : "text-slate-400 hover:text-slate-200"
            }`}
          >
            <FileCode2 className="w-3.5 h-3.5" /> Java Source Tree ({javaFiles.length})
          </button>
          <button
            onClick={() => setActiveTab("deploy")}
            className={`px-3 py-1.5 rounded-md transition-all flex items-center gap-1.5 ${
              activeTab === "deploy" ? "bg-emerald-600 text-white shadow-sm font-semibold" : "text-slate-400 hover:text-slate-200"
            }`}
          >
            <Layers className="w-3.5 h-3.5" /> Docker &amp; K8s Manifests
          </button>
        </div>

        <button
          onClick={loadData}
          disabled={loading}
          className="px-3 py-1.5 rounded bg-slate-800 hover:bg-slate-700 text-xs border border-slate-700 text-slate-300 flex items-center gap-1.5"
        >
          <RefreshCw className={`w-3 h-3 ${loading ? "animate-spin" : ""}`} /> Refresh
        </button>
      </header>

      {/* Main Content Area */}
      <main className="flex-1 p-6 max-w-7xl w-full mx-auto space-y-6">
        
        {/* VIEW 1: MICROSERVICES TOPOLOGY */}
        {activeTab === "services" && (
          <div className="space-y-6">
            {/* Architecture Overview Banner */}
            <div className="bg-slate-900 border border-slate-800 rounded-xl p-5 shadow-sm">
              <div className="flex items-center justify-between mb-4 pb-3 border-b border-slate-800/80">
                <div className="flex items-center gap-2">
                  <Activity className="w-4 h-4 text-emerald-400" />
                  <h2 className="text-sm font-bold text-slate-200 uppercase tracking-wider">
                    Core Business Microservices Registry
                  </h2>
                </div>
                <div className="text-xs text-slate-400 flex items-center gap-3">
                  <span className="flex items-center gap-1 text-emerald-400">
                    <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse"></span>
                    Eureka Discovery: UP
                  </span>
                  <span>Port 8000: API Gateway</span>
                </div>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                {arch?.microservices.map((svc, idx) => (
                  <div
                    key={idx}
                    className="p-4 rounded-lg bg-slate-950/70 border border-slate-800 hover:border-slate-700 transition-all flex flex-col justify-between"
                  >
                    <div>
                      <div className="flex items-center justify-between mb-2">
                        <span className="text-sm font-bold text-emerald-400 flex items-center gap-1.5">
                          <Cpu className="w-3.5 h-3.5 text-slate-400" />
                          {svc.name}
                        </span>
                        <span className="px-2 py-0.5 rounded text-[10px] font-semibold bg-slate-800 text-slate-300 border border-slate-700">
                          :{svc.port}
                        </span>
                      </div>
                      <p className="text-xs text-slate-400 mb-3 font-sans">
                        <span className="text-slate-300 font-semibold">Storage:</span> {svc.storage}
                      </p>
                      
                      <div className="space-y-1 mb-3">
                        <span className="text-[10px] uppercase font-bold text-slate-400">Core Features:</span>
                        <ul className="text-xs text-slate-300 space-y-0.5 list-disc list-inside">
                          {svc.features.map((f, i) => (
                            <li key={i} className="truncate">{f}</li>
                          ))}
                        </ul>
                      </div>
                    </div>

                    {svc.events && (
                      <div className="pt-2 border-t border-slate-900 flex flex-wrap gap-1">
                        {svc.events.map((e, ei) => (
                          <span key={ei} className="px-1.5 py-0.5 rounded text-[9px] bg-indigo-950 text-indigo-300 border border-indigo-800/40">
                            {e}
                          </span>
                        ))}
                      </div>
                    )}
                  </div>
                ))}
              </div>
            </div>

            {/* Layer-by-Layer Tech Stack Grid */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              <div className="p-4 rounded-xl bg-slate-900 border border-slate-800">
                <div className="flex items-center gap-2 mb-3 text-emerald-400">
                  <Database className="w-4 h-4" />
                  <h3 className="text-xs font-bold uppercase tracking-wider">Persistence &amp; Cache Layer</h3>
                </div>
                <ul className="text-xs space-y-2 text-slate-300 font-sans">
                  <li>&bull; <strong className="text-slate-100 font-mono">PostgreSQL 16:</strong> ACID Transactions, Liquibase/Flyway migrations, HikariCP pool.</li>
                  <li>&bull; <strong className="text-slate-100 font-mono">Redis 7.2 + Redisson:</strong> Geospatial driver telemetry &amp; Distributed locks.</li>
                </ul>
              </div>

              <div className="p-4 rounded-xl bg-slate-900 border border-slate-800">
                <div className="flex items-center gap-2 mb-3 text-indigo-400">
                  <Radio className="w-4 h-4" />
                  <h3 className="text-xs font-bold uppercase tracking-wider">Messaging &amp; Reliability</h3>
                </div>
                <ul className="text-xs space-y-2 text-slate-300 font-sans">
                  <li>&bull; <strong className="text-slate-100 font-mono">Apache Kafka 3.6:</strong> Event-driven topics with partition keys.</li>
                  <li>&bull; <strong className="text-slate-100 font-mono">Transactional Outbox:</strong> Zero-data-loss guaranteed event publishing.</li>
                  <li>&bull; <strong className="text-slate-100 font-mono">Resilience4j:</strong> Circuit breaker &amp; automated rate limiters.</li>
                </ul>
              </div>

              <div className="p-4 rounded-xl bg-slate-900 border border-slate-800">
                <div className="flex items-center gap-2 mb-3 text-amber-400">
                  <ShieldCheck className="w-4 h-4" />
                  <h3 className="text-xs font-bold uppercase tracking-wider">Security &amp; Observability</h3>
                </div>
                <ul className="text-xs space-y-2 text-slate-300 font-sans">
                  <li>&bull; <strong className="text-slate-100 font-mono">Spring Security 6 + Keycloak:</strong> Stateless JWT AuthN/AuthZ.</li>
                  <li>&bull; <strong className="text-slate-100 font-mono">Prometheus &amp; Grafana:</strong> Real-time operational metrics &amp; alerts.</li>
                  <li>&bull; <strong className="text-slate-100 font-mono">OpenTelemetry / Zipkin:</strong> Distributed trace latency correlation.</li>
                </ul>
              </div>
            </div>
          </div>
        )}

        {/* VIEW 2: LIVE REST API TESTER */}
        {activeTab === "api" && (
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
            {/* Left side: Predefined endpoints and input form */}
            <div className="lg:col-span-5 space-y-4">
              <div className="p-4 rounded-xl bg-slate-900 border border-slate-800 space-y-3">
                <h3 className="text-xs font-bold uppercase tracking-wider text-slate-400">
                  Quick-Run Preset Requests
                </h3>
                <div className="space-y-1.5">
                  {predefinedTests.map((pt, i) => (
                    <button
                      key={i}
                      onClick={() => {
                        setTestMethod(pt.method);
                        setTestEndpoint(pt.url);
                        setTestBody(pt.body ? JSON.stringify(pt.body, null, 2) : "");
                      }}
                      className="w-full text-left p-2.5 rounded-lg bg-slate-950 border border-slate-800/80 hover:border-emerald-500/50 hover:bg-slate-900 transition-all text-xs flex items-center justify-between"
                    >
                      <span className="text-slate-200 truncate">{pt.label}</span>
                      <span className={`px-1.5 py-0.5 rounded text-[10px] font-bold ${
                        pt.method === "GET" ? "bg-sky-950 text-sky-400 border border-sky-800" : "bg-emerald-950 text-emerald-400 border border-emerald-800"
                      }`}>
                        {pt.method}
                      </span>
                    </button>
                  ))}
                </div>
              </div>

              {/* Endpoint Runner Form */}
              <div className="p-4 rounded-xl bg-slate-900 border border-slate-800 space-y-3">
                <div className="flex gap-2">
                  <select
                    value={testMethod}
                    onChange={(e) => setTestMethod(e.target.value)}
                    className="bg-slate-950 border border-slate-800 rounded px-2.5 py-2 text-xs font-bold text-emerald-400 outline-none"
                  >
                    <option value="GET">GET</option>
                    <option value="POST">POST</option>
                    <option value="PUT">PUT</option>
                    <option value="DELETE">DELETE</option>
                  </select>
                  <input
                    type="text"
                    value={testEndpoint}
                    onChange={(e) => setTestEndpoint(e.target.value)}
                    className="flex-1 bg-slate-950 border border-slate-800 rounded px-3 py-2 text-xs text-slate-200 outline-none focus:border-emerald-500"
                  />
                  <button
                    onClick={runApiTest}
                    className="px-4 py-2 bg-emerald-600 hover:bg-emerald-500 text-white rounded font-bold text-xs flex items-center gap-1.5 shadow-sm"
                  >
                    <Play className="w-3.5 h-3.5 fill-current" /> Run
                  </button>
                </div>

                {(testMethod === "POST" || testMethod === "PUT") && (
                  <div>
                    <label className="text-[11px] text-slate-400 block mb-1">Request Payload (JSON)</label>
                    <textarea
                      rows={9}
                      value={testBody}
                      onChange={(e) => setTestBody(e.target.value)}
                      className="w-full bg-slate-950 border border-slate-800 rounded p-3 text-xs text-emerald-300 font-mono outline-none focus:border-emerald-500"
                    />
                  </div>
                )}
              </div>
            </div>

            {/* Right side: Live Response Box */}
            <div className="lg:col-span-7 flex flex-col p-4 rounded-xl bg-slate-900 border border-slate-800">
              <div className="flex items-center justify-between pb-3 mb-3 border-b border-slate-800">
                <span className="text-xs font-bold text-slate-300 flex items-center gap-2">
                  <Terminal className="w-4 h-4 text-emerald-400" />
                  Live API Server Response
                </span>
                <button
                  onClick={() => navigator.clipboard.writeText(apiResponse)}
                  className="text-xs text-slate-400 hover:text-slate-200 flex items-center gap-1"
                >
                  <Copy className="w-3 h-3" /> Copy JSON
                </button>
              </div>
              <pre className="flex-1 overflow-auto bg-slate-950 border border-slate-800/80 rounded-lg p-4 text-xs text-emerald-400 leading-relaxed font-mono whitespace-pre-wrap">
                {apiResponse}
              </pre>
            </div>
          </div>
        )}

        {/* VIEW 3: JAVA SOURCE CODE EXPLORER */}
        {activeTab === "source" && (
          <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 min-h-[550px]">
            {/* File Directory List */}
            <div className="lg:col-span-4 p-3 rounded-xl bg-slate-900 border border-slate-800 overflow-y-auto max-h-[600px] space-y-1">
              <div className="text-xs font-bold uppercase tracking-wider text-slate-400 px-2 py-1.5 border-b border-slate-800 mb-2">
                Spring Boot 3.4 Microservices Source
              </div>
              {javaFiles.map((file, idx) => (
                <button
                  key={idx}
                  onClick={() => setSelectedFile(file)}
                  className={`w-full text-left px-2.5 py-1.5 rounded text-xs flex items-center gap-2 transition-all ${
                    selectedFile?.relativePath === file.relativePath
                      ? "bg-emerald-600/20 text-emerald-300 border border-emerald-500/40 font-semibold"
                      : "text-slate-400 hover:bg-slate-950 hover:text-slate-200"
                  }`}
                >
                  <Code className="w-3.5 h-3.5 shrink-0 text-slate-400" />
                  <span className="truncate">{file.relativePath.replace("microservices/", "")}</span>
                </button>
              ))}
            </div>

            {/* Code Viewer */}
            <div className="lg:col-span-8 p-4 rounded-xl bg-slate-900 border border-slate-800 flex flex-col">
              <div className="flex items-center justify-between pb-3 mb-3 border-b border-slate-800">
                <span className="text-xs font-bold text-slate-200 font-mono truncate">
                  {selectedFile ? selectedFile.relativePath : "Select a source file"}
                </span>
                {selectedFile && (
                  <button
                    onClick={() => navigator.clipboard.writeText(selectedFile.content)}
                    className="text-xs text-slate-400 hover:text-slate-200 flex items-center gap-1"
                  >
                    <Copy className="w-3 h-3" /> Copy Code
                  </button>
                )}
              </div>
              <pre className="flex-1 overflow-auto bg-slate-950 border border-slate-800/80 rounded-lg p-4 text-xs text-slate-200 leading-relaxed font-mono whitespace-pre max-h-[550px]">
                {selectedFile ? selectedFile.content : "// No file selected"}
              </pre>
            </div>
          </div>
        )}

        {/* VIEW 4: DOCKER & K8S MANIFESTS */}
        {activeTab === "deploy" && (
          <div className="space-y-4">
            <div className="p-4 rounded-xl bg-slate-900 border border-slate-800">
              <div className="flex items-center justify-between mb-3 pb-2 border-b border-slate-800">
                <span className="text-xs font-bold text-slate-200 flex items-center gap-2">
                  <Boxes className="w-4 h-4 text-emerald-400" />
                  docker-compose.yml (Complete Multi-Container Cluster)
                </span>
                <span className="text-xs text-slate-400">PostgreSQL, Redis, Kafka, Eureka, Microservices</span>
              </div>
              <pre className="p-4 bg-slate-950 rounded-lg border border-slate-800 text-xs text-sky-300 font-mono max-h-[400px] overflow-auto">
{`version: '3.8'
services:
  service-registry:     # Netflix Eureka (Port 8761)
  api-gateway:          # Spring Cloud Gateway (Port 8000)
  order-service:        # Order Management & Outbox (Port 8081)
  pickup-fleet-service: # Fleet & Dispatch (Port 8082)
  fulfillment-service:  # Hub Transit & POD (Port 8083)
  tracking-service:     # Realtime GPS & Redis Geo (Port 8084)
  notification-service: # Async SMS/Email (Port 8085)
  user-auth-service:    # IAM & JWT (Port 8080)
  postgres:             # PostgreSQL 16 (Port 5432)
  redis:                # Redis 7.2 (Port 6379)
  kafka:                # Apache Kafka 3.6 (Port 9092)
  prometheus:           # Metrics Scraper (Port 9090)
  grafana:              # Dashboards (Port 3001)`}
              </pre>
            </div>
          </div>
        )}
      </main>

      {/* Footer Status Bar */}
      <footer className="border-t border-slate-800 bg-slate-900/60 px-6 py-2 text-xs text-slate-400 flex items-center justify-between">
        <div className="flex items-center gap-4">
          <span className="flex items-center gap-1.5 text-emerald-400">
            <span className="w-2 h-2 rounded-full bg-emerald-500"></span> Active API Runner (Port 3000)
          </span>
          <span>&bull;</span>
          <span>PostgreSQL 16 Multi-DB Pool: Healthy</span>
          <span>&bull;</span>
          <span>Kafka Cluster: 3 Brokers Synced</span>
        </div>
        <div className="text-slate-400 font-sans">
          Logistics Microservices Enterprise Edition &bull; Spring Boot 3.4
        </div>
      </footer>
    </div>
  );
}
