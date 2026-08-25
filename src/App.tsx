import React from 'react';
import { Server, Database, Shield, Radio, Layers, Cpu, Terminal } from 'lucide-react';

export function App() {
  const microservices = [
    { name: 'API Gateway', port: 8000, desc: 'Spring Cloud Gateway, Routing & Rate Limiting' },
    { name: 'Eureka Registry', port: 8761, desc: 'Service Discovery & Health Monitoring' },
    { name: 'User Auth Service', port: 8081, desc: 'Keycloak OIDC IAM, JWT & Role-Based Access' },
    { name: 'Order Service', port: 8082, desc: 'Order Lifecycle, Pricing & Distributed Locking' },
    { name: 'Pickup & Fleet Service', port: 8083, desc: 'Driver Matching & Dispatch Optimization' },
    { name: 'Fulfillment Service', port: 8084, desc: 'Hub Sorting, Linehaul Transit & POD' },
    { name: 'Tracking & Telemetry', port: 8085, desc: 'Elasticsearch Geo-Search & GPS Tracking' },
    { name: 'Notification Service', port: 8086, desc: 'Kafka Event Consumer & Multi-channel Alerts' },
  ];

  return (
    <div id="backend-root" className="min-h-screen bg-slate-950 text-slate-100 p-8 font-sans">
      <div className="max-w-5xl mx-auto space-y-8">
        <header id="backend-header" className="border-b border-slate-800 pb-6">
          <div className="flex items-center gap-3">
            <Server className="w-8 h-8 text-indigo-400" />
            <div>
              <h1 className="text-2xl font-bold tracking-tight text-white">Logistics Microservices Backend</h1>
              <p className="text-sm text-slate-400">Pure Spring Boot 3.x, Java 17, Keycloak 24, Kafka & Elasticsearch Architecture</p>
            </div>
          </div>
        </header>

        <div id="backend-info-banner" className="p-4 rounded-lg bg-slate-900 border border-slate-800 text-sm text-slate-300 flex items-center gap-3">
          <Terminal className="w-5 h-5 text-emerald-400 flex-shrink-0" />
          <span>All frontend consumer UI modules have been removed. This workspace is configured as a pure backend microservices repository.</span>
        </div>

        <section id="microservices-grid" className="space-y-4">
          <h2 className="text-lg font-semibold text-slate-200 flex items-center gap-2">
            <Layers className="w-5 h-5 text-indigo-400" />
            Active Microservices Topology
          </h2>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {microservices.map((svc) => (
              <div
                key={svc.name}
                id={`svc-${svc.port}`}
                className="p-4 rounded-lg bg-slate-900/60 border border-slate-800 flex items-start justify-between"
              >
                <div>
                  <h3 className="font-medium text-slate-100">{svc.name}</h3>
                  <p className="text-xs text-slate-400 mt-1">{svc.desc}</p>
                </div>
                <span className="text-xs font-mono px-2.5 py-1 rounded bg-slate-800 text-indigo-300 border border-slate-700">
                  :{svc.port}
                </span>
              </div>
            ))}
          </div>
        </section>

        <footer id="backend-footer" className="text-xs text-slate-500 pt-6 border-t border-slate-900 flex justify-between">
          <span>Backend Maven Root: <code className="font-mono text-slate-400">/microservices/pom.xml</code></span>
          <span>Docker Compose: <code className="font-mono text-slate-400">/docker-compose.yml</code></span>
        </footer>
      </div>
    </div>
  );
}

export default App;
