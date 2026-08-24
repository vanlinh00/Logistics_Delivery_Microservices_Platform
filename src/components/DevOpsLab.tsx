import React, { useState, useEffect } from 'react';
import {
  Boxes,
  Cpu,
  Layers,
  Terminal,
  Play,
  RefreshCw,
  CheckCircle2,
  AlertCircle,
  Copy,
  Check,
  ShieldCheck,
  Zap,
  Server,
  Cloud,
  FileCode,
  Gauge,
  ArrowRight,
  GitBranch,
  Container,
  Activity,
  Sliders,
  Database
} from 'lucide-react';

export const DevOpsLab: React.FC = () => {
  const [activeSection, setActiveSection] = useState<'cicd' | 'docker' | 'k8s' | 'commands'>('cicd');
  const [selectedCiProvider, setSelectedCiProvider] = useState<'github' | 'jenkins' | 'gitlab'>('github');
  const [pipelineState, setPipelineState] = useState<'idle' | 'running' | 'completed'>('idle');
  const [currentStepIndex, setCurrentStepIndex] = useState(-1);
  const [pipelineLogs, setPipelineLogs] = useState<string[]>([]);
  const [copiedSnippet, setCopiedSnippet] = useState<string | null>(null);

  // K8s interactive scaling state
  const [cpuLoad, setCpuLoad] = useState<number>(45);
  const [activeReplicas, setActiveReplicas] = useState<number>(3);
  const [k8sTab, setK8sTab] = useState<'topology' | 'manifests' | 'probes'>('topology');

  // Docker comparison tab
  const [dockerView, setDockerView] = useState<'multistage' | 'layers' | 'jvm'>('multistage');

  const copyToClipboard = (text: string, id: string) => {
    navigator.clipboard.writeText(text);
    setCopiedSnippet(id);
    setTimeout(() => setCopiedSnippet(null), 2000);
  };

  // Simulate CI/CD Pipeline Steps
  const PIPELINE_STEPS = [
    {
      id: 'lint_test',
      title: '1. Maven Unit & Integration Tests',
      desc: 'Parallel test execution across 7 microservices + React Vitest',
      logs: [
        '🚀 [CI/CD] Triggered by push to branch main (commit 4f9a2b1)',
        '📦 Maven 3.9.6 & Eclipse Temurin 21 initialized',
        '🧪 [order-service] Running OrderSagaCoordinatorTest... PASS (1.2s)',
        '🧪 [fleet-service] Running ParallelDriverMatchingTest... PASS (0.8s)',
        '🧪 [tracking-service] Running AsyncTrackingAggregatorTest... PASS (0.6s)',
        '⚛️ [frontend] Vite TypeScript typecheck & bundle verification... PASS',
        '✅ Tests Completed: 48 passed, 0 failures, 0 skipped'
      ]
    },
    {
      id: 'security_scan',
      title: '2. SAST & Container Vulnerability Scan',
      desc: 'Trivy FS security scan, OWASP dependency check & SonarQube quality gate',
      logs: [
        '🛡️ Initializing Aquasec Trivy 0.49 Scanner...',
        '🔍 Scanning root filesystem and Maven dependencies...',
        '📊 Vulnerability Summary: 0 CRITICAL, 0 HIGH, 2 LOW (ignored)',
        '📈 SonarQube Code Quality Gate: PASSED (Coverage 88.4%, 0 Code Smells)',
        '🔒 Checkmarx Secrets Scan: PASSED (No hardcoded credentials found)'
      ]
    },
    {
      id: 'docker_build',
      title: '3. Docker Multi-Stage Build & Push',
      desc: 'Buildx multi-arch compilation with GitHub Actions layer cache to GHCR',
      logs: [
        '🐳 Docker Buildx initialized with builder [multiarch-builder-0]',
        '📥 Stage 1: Using cached Maven dependencies (Layer hash: 8e3c1a9)',
        '⚙️ Stage 1: Compiling microservice JARs in 14.2s...',
        '📦 Stage 2: Packaging lightweight Alpine JRE 21 with non-root appuser:appgroup (UID 10001)',
        '🏷️ Tagged: ghcr.io/logistics-org/order-service:v1.0.0-sha-4f9a2b1',
        '🚀 Pushing compressed image (Size: 162.4MB) to GitHub Container Registry... DONE'
      ]
    },
    {
      id: 'k8s_deploy',
      title: '4. GitOps K8s Zero-Downtime Rollout',
      desc: 'Kustomize image substitution & RollingUpdate rollout with health probes',
      logs: [
        '☸️ Authenticating with Kubernetes cluster [k8s-prod-cluster-sea-1]',
        '🔧 Kustomize set image ghcr.io/logistics-org/order-service:v1.0.0-sha-4f9a2b1',
        '🚀 Applying manifests to namespace [logistics-prod]...',
        '🔄 RollingUpdate strategy: maxSurge=1, maxUnavailable=0',
        '🟢 Pod order-service-678f9-abcde: StartupProbe PASSED (5s)',
        '🟢 Pod order-service-678f9-abcde: ReadinessProbe PASSED (200 OK at /actuator/health/readiness)',
        '🔻 Terminating old pod order-service-542a1-xyz12 gracefully...',
        '🎉 Rollout completed successfully! 3/3 Replicas live with zero downtime.'
      ]
    }
  ];

  const runPipelineSimulation = () => {
    setPipelineState('running');
    setCurrentStepIndex(0);
    setPipelineLogs([]);

    let step = 0;
    const interval = setInterval(() => {
      if (step < PIPELINE_STEPS.length) {
        setCurrentStepIndex(step);
        setPipelineLogs(prev => [...prev, ...PIPELINE_STEPS[step].logs]);
        step++;
      } else {
        clearInterval(interval);
        setPipelineState('completed');
      }
    }, 1200);
  };

  // Adjust HPA Replicas dynamically based on CPU load slider
  useEffect(() => {
    if (cpuLoad < 40) {
      setActiveReplicas(2);
    } else if (cpuLoad < 65) {
      setActiveReplicas(3);
    } else if (cpuLoad < 80) {
      setActiveReplicas(5);
    } else if (cpuLoad < 92) {
      setActiveReplicas(8);
    } else {
      setActiveReplicas(12);
    }
  }, [cpuLoad]);

  return (
    <div className="space-y-6">
      {/* Top Banner / Concept Header */}
      <div className="bg-slate-900/90 border border-slate-800 rounded-xl p-5 space-y-4">
        <div className="flex flex-wrap items-center justify-between gap-4">
          <div className="flex items-center gap-3">
            <div className="h-10 w-10 rounded-xl bg-blue-500/10 border border-blue-500/20 flex items-center justify-center text-blue-400">
              <Boxes className="h-6 w-6" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h2 className="text-base font-bold text-white tracking-wide">
                  DevOps, Docker & Kubernetes Production Suite
                </h2>
                <span className="px-2 py-0.5 text-[10px] font-mono bg-blue-500/10 text-blue-400 border border-blue-500/20 rounded">
                  Cloud Native
                </span>
              </div>
              <p className="text-xs text-slate-400">
                Automated CI/CD Pipelines, Multi-Stage Container Layer Optimization, Zero-Downtime Rolling Deployments & Kubernetes HPA Autoscaling
              </p>
            </div>
          </div>

          {/* Section Navigation Tabs */}
          <div className="flex items-center gap-1.5 bg-slate-950 p-1 rounded-lg border border-slate-800">
            <button
              onClick={() => setActiveSection('cicd')}
              className={`px-3 py-1.5 text-xs font-medium rounded-md transition-all flex items-center gap-1.5 ${
                activeSection === 'cicd' ? 'bg-blue-600 text-white shadow' : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              <GitBranch className="h-3.5 w-3.5" />
              CI/CD Pipelines
            </button>
            <button
              onClick={() => setActiveSection('docker')}
              className={`px-3 py-1.5 text-xs font-medium rounded-md transition-all flex items-center gap-1.5 ${
                activeSection === 'docker' ? 'bg-indigo-600 text-white shadow' : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              <Container className="h-3.5 w-3.5" />
              Docker Multi-Stage
            </button>
            <button
              onClick={() => setActiveSection('k8s')}
              className={`px-3 py-1.5 text-xs font-medium rounded-md transition-all flex items-center gap-1.5 ${
                activeSection === 'k8s' ? 'bg-cyan-600 text-white shadow' : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              <Cloud className="h-3.5 w-3.5" />
              Kubernetes Cluster & HPA
            </button>
            <button
              onClick={() => setActiveSection('commands')}
              className={`px-3 py-1.5 text-xs font-medium rounded-md transition-all flex items-center gap-1.5 ${
                activeSection === 'commands' ? 'bg-emerald-600 text-white shadow' : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              <Terminal className="h-3.5 w-3.5" />
              CLI Runbook
            </button>
          </div>
        </div>
      </div>

      {/* SECTION 1: CI/CD PIPELINES */}
      {activeSection === 'cicd' && (
        <div className="space-y-6">
          {/* Controls & Simulator */}
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
            {/* Left: Pipeline Interactive Steps */}
            <div className="lg:col-span-1 bg-slate-900/80 border border-slate-800 rounded-xl p-5 space-y-4 flex flex-col justify-between">
              <div className="space-y-4">
                <div className="flex items-center justify-between">
                  <h3 className="font-semibold text-white text-sm flex items-center gap-2">
                    <Zap className="h-4 w-4 text-amber-400" />
                    Interactive Pipeline Runner
                  </h3>
                  <span className={`px-2 py-0.5 text-[10px] rounded font-mono font-medium ${
                    pipelineState === 'running' ? 'bg-amber-950 text-amber-300 border border-amber-800 animate-pulse' :
                    pipelineState === 'completed' ? 'bg-emerald-950 text-emerald-300 border border-emerald-800' :
                    'bg-slate-800 text-slate-400'
                  }`}>
                    {pipelineState.toUpperCase()}
                  </span>
                </div>

                <p className="text-xs text-slate-400 leading-relaxed">
                  Mô phỏng quy trình tự động hóa CI/CD từ lúc lập trình viên Push Git Commit đến khi triển khai Zero-Downtime lên Kubernetes.
                </p>

                {/* Steps Visual List */}
                <div className="space-y-2.5">
                  {PIPELINE_STEPS.map((step, idx) => {
                    const isCurrent = currentStepIndex === idx && pipelineState === 'running';
                    const isDone = currentStepIndex > idx || pipelineState === 'completed';

                    return (
                      <div
                        key={step.id}
                        className={`p-3 rounded-lg border text-xs transition-all ${
                          isCurrent
                            ? 'bg-blue-950/40 border-blue-500/50 text-blue-200 shadow-sm'
                            : isDone
                            ? 'bg-slate-950/80 border-emerald-800/60 text-slate-300'
                            : 'bg-slate-950/40 border-slate-800/60 text-slate-500'
                        }`}
                      >
                        <div className="flex items-center justify-between mb-1">
                          <span className="font-semibold flex items-center gap-2">
                            {isDone ? (
                              <CheckCircle2 className="h-3.5 w-3.5 text-emerald-400 shrink-0" />
                            ) : isCurrent ? (
                              <RefreshCw className="h-3.5 w-3.5 text-blue-400 animate-spin shrink-0" />
                            ) : (
                              <div className="h-3.5 w-3.5 rounded-full border border-slate-600 shrink-0" />
                            )}
                            {step.title}
                          </span>
                          <span className="text-[10px] font-mono text-slate-500">Stage {idx + 1}</span>
                        </div>
                        <p className="text-[11px] text-slate-400 pl-5">{step.desc}</p>
                      </div>
                    );
                  })}
                </div>
              </div>

              <button
                onClick={runPipelineSimulation}
                disabled={pipelineState === 'running'}
                className="w-full py-2.5 px-4 bg-blue-600 hover:bg-blue-500 disabled:opacity-50 text-white rounded-lg text-xs font-semibold flex items-center justify-center gap-2 transition-all mt-4"
              >
                {pipelineState === 'running' ? (
                  <>
                    <RefreshCw className="h-3.5 w-3.5 animate-spin" /> Đang chạy Pipeline Stages...
                  </>
                ) : (
                  <>
                    <Play className="h-3.5 w-3.5" /> Trigger CI/CD Pipeline (git push main)
                  </>
                )}
              </button>
            </div>

            {/* Right: Live Build Logs Terminal */}
            <div className="lg:col-span-2 bg-slate-950 border border-slate-800 rounded-xl p-4 flex flex-col justify-between font-mono">
              <div className="space-y-2">
                <div className="flex items-center justify-between pb-2 border-b border-slate-800">
                  <div className="flex items-center gap-2 text-xs text-slate-400">
                    <Terminal className="h-4 w-4 text-emerald-400" />
                    <span>Live CI/CD Console Output (runner-ubuntu-latest)</span>
                  </div>
                  <div className="flex items-center gap-1.5">
                    <div className="h-2.5 w-2.5 rounded-full bg-red-500/80" />
                    <div className="h-2.5 w-2.5 rounded-full bg-amber-500/80" />
                    <div className="h-2.5 w-2.5 rounded-full bg-emerald-500/80" />
                  </div>
                </div>

                <div className="h-72 overflow-y-auto space-y-1 text-[11px] p-2 text-slate-300 leading-relaxed">
                  {pipelineLogs.length === 0 ? (
                    <div className="h-full flex flex-col items-center justify-center text-slate-600 space-y-2">
                      <Terminal className="h-8 w-8 text-slate-700" />
                      <p>Nhấn "Trigger CI/CD Pipeline" để bắt đầu quá trình Build & Deploy tự động</p>
                    </div>
                  ) : (
                    pipelineLogs.map((line, i) => (
                      <div key={i} className="flex items-start gap-2">
                        <span className="text-slate-600 select-none">{String(i + 1).padStart(2, '0')}</span>
                        <span className={
                          line.includes('PASS') || line.includes('DONE') || line.includes('🎉') ? 'text-emerald-400 font-medium' :
                          line.includes('🚀') || line.includes('🐳') || line.includes('☸️') ? 'text-cyan-300' :
                          line.includes('🛡️') || line.includes('🔍') ? 'text-amber-300' :
                          'text-slate-300'
                        }>
                          {line}
                        </span>
                      </div>
                    ))
                  )}
                </div>
              </div>

              <div className="pt-2 border-t border-slate-800 flex items-center justify-between text-[11px] text-slate-500">
                <span>Branch: <code className="text-blue-400">main</code></span>
                <span>Registry: <code className="text-emerald-400">ghcr.io/logistics-org</code></span>
                <span>Cluster: <code className="text-purple-400">logistics-prod (GKE/EKS)</code></span>
              </div>
            </div>
          </div>

          {/* CI/CD Configuration Viewer */}
          <div className="bg-slate-900/80 border border-slate-800 rounded-xl p-5 space-y-4">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <div className="flex items-center gap-2">
                <FileCode className="h-4 w-4 text-blue-400" />
                <h3 className="font-semibold text-white text-sm">Enterprise CI/CD Workflow Specs</h3>
              </div>

              <div className="flex items-center gap-1.5 bg-slate-950 p-1 rounded-lg border border-slate-800 text-xs">
                <button
                  onClick={() => setSelectedCiProvider('github')}
                  className={`px-3 py-1 rounded transition-all font-mono ${
                    selectedCiProvider === 'github' ? 'bg-blue-600 text-white' : 'text-slate-400 hover:text-slate-200'
                  }`}
                >
                  .github/workflows/ci-cd.yml
                </button>
                <button
                  onClick={() => setSelectedCiProvider('jenkins')}
                  className={`px-3 py-1 rounded transition-all font-mono ${
                    selectedCiProvider === 'jenkins' ? 'bg-blue-600 text-white' : 'text-slate-400 hover:text-slate-200'
                  }`}
                >
                  Jenkinsfile
                </button>
                <button
                  onClick={() => setSelectedCiProvider('gitlab')}
                  className={`px-3 py-1 rounded transition-all font-mono ${
                    selectedCiProvider === 'gitlab' ? 'bg-blue-600 text-white' : 'text-slate-400 hover:text-slate-200'
                  }`}
                >
                  .gitlab-ci.yml
                </button>
              </div>
            </div>

            <div className="relative">
              <div className="bg-slate-950 p-4 rounded-lg border border-slate-800 font-mono text-xs text-slate-300 overflow-x-auto max-h-96">
                <pre>
                  {selectedCiProvider === 'github' ? `name: Enterprise Logistics CI/CD Pipeline
on:
  push:
    branches: [ main, develop ]
    tags: [ 'v*.*.*' ]
  pull_request:
    branches: [ main ]

jobs:
  # STAGE 1: Parallel Matrix Unit Testing
  test-microservices:
    strategy:
      matrix:
        service: [ order-service, user-auth-service, pickup-fleet-service, tracking-service, notification-service ]
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { java-version: '21', distribution: 'temurin', cache: maven }
      - run: mvn clean test -pl \${{ matrix.service }} -am -B

  # STAGE 2: Security & SAST Vulnerability Scanning
  security-scan:
    needs: [test-microservices]
    steps:
      - uses: aquasecurity/trivy-action@master
        with: { scan-type: 'fs', severity: 'CRITICAL,HIGH' }

  # STAGE 3: Docker Multi-Stage Build & Layer Caching
  build-and-push-images:
    needs: [security-scan]
    steps:
      - uses: docker/build-push-action@v5
        with:
          context: ./microservices
          file: microservices/order-service/Dockerfile
          push: true
          tags: ghcr.io/logistics-org/order-service:\${{ github.sha }}
          cache-from: type=gha
          cache-to: type=gha,mode=max

  # STAGE 4: GitOps Kubernetes Zero-Downtime Rollout
  deploy-kubernetes:
    needs: [build-and-push-images]
    steps:
      - run: |
          kustomize edit set image ghcr.io/logistics-org/order-service:\${{ github.sha }}
          kubectl apply -k ./infrastructure/k8s
          kubectl rollout status deployment/order-service-deployment -n logistics-prod`
                  : selectedCiProvider === 'jenkins' ? `pipeline {
    agent any
    environment {
        DOCKER_REGISTRY = 'ghcr.io/logistics-org'
        K8S_NAMESPACE   = 'logistics-prod'
    }
    stages {
        stage('Parallel Build & Unit Tests') {
            parallel {
                stage('Order Service') { steps { sh 'mvn clean test -pl order-service -am -B' } }
                stage('Fleet & Tracking') { steps { sh 'mvn clean test -pl pickup-fleet-service,tracking-service -am -B' } }
            }
        }
        stage('Security Analysis') {
            steps { sh 'trivy fs --severity HIGH,CRITICAL .' }
        }
        stage('Docker Multi-Stage Build & Push') {
            steps { sh 'docker build -t \${DOCKER_REGISTRY}/order-service:\${BUILD_NUMBER} -f microservices/order-service/Dockerfile microservices' }
        }
        stage('Kubernetes GitOps Deployment') {
            steps {
                sh 'kubectl set image deployment/order-service-deployment order-service=\${DOCKER_REGISTRY}/order-service:\${BUILD_NUMBER} -n \${K8S_NAMESPACE}'
                sh 'kubectl rollout status deployment/order-service-deployment -n \${K8S_NAMESPACE}'
            }
        }
    }
}`
                  : `stages:
  - test
  - security
  - package
  - deploy

unit-test-java:
  stage: test
  image: maven:3.9.6-eclipse-temurin-21-alpine
  script:
    - mvn test -B

container-scanning:
  stage: security
  image: aquasec/trivy:latest
  script:
    - trivy fs --severity HIGH,CRITICAL .

docker-build-push:
  stage: package
  image: docker:24.0.5
  script:
    - docker build -t $CI_REGISTRY_IMAGE:$CI_COMMIT_SHORT_SHA -f microservices/order-service/Dockerfile microservices
    - docker push $CI_REGISTRY_IMAGE:$CI_COMMIT_SHORT_SHA

k8s-deploy:
  stage: deploy
  image: bitnami/kubectl:latest
  script:
    - kubectl set image deployment/order-service-deployment order-service=$CI_REGISTRY_IMAGE:$CI_COMMIT_SHORT_SHA -n logistics-prod
    - kubectl rollout status deployment/order-service-deployment -n logistics-prod`}
                </pre>
              </div>

              <button
                onClick={() => copyToClipboard(
                  selectedCiProvider === 'github' ? 'gh workflow run' : selectedCiProvider,
                  'ci-config'
                )}
                className="absolute top-3 right-3 p-2 bg-slate-800 hover:bg-slate-700 text-slate-300 rounded-lg text-xs flex items-center gap-1.5 transition-all"
              >
                {copiedSnippet === 'ci-config' ? <Check className="h-3.5 w-3.5 text-emerald-400" /> : <Copy className="h-3.5 w-3.5" />}
                {copiedSnippet === 'ci-config' ? 'Đã sao chép' : 'Copy Manifest'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* SECTION 2: DOCKER MULTI-STAGE & OPTIMIZATION */}
      {activeSection === 'docker' && (
        <div className="space-y-6">
          {/* Comparison Cards */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div className="bg-slate-900/80 border border-slate-800 rounded-xl p-4 space-y-2">
              <div className="flex items-center justify-between">
                <span className="text-xs text-slate-400 font-semibold">Image Size Optimization</span>
                <span className="text-xs font-mono text-emerald-400 font-bold">-76% SLIMMER</span>
              </div>
              <div className="space-y-1.5">
                <div className="flex items-center justify-between text-xs">
                  <span className="text-slate-400">Standard Fat Image:</span>
                  <span className="font-mono text-red-400 line-through">685 MB</span>
                </div>
                <div className="flex items-center justify-between text-xs">
                  <span className="text-slate-200 font-semibold">Multi-Stage Alpine JRE:</span>
                  <span className="font-mono text-emerald-400 font-bold">162 MB</span>
                </div>
              </div>
              <div className="w-full bg-slate-800 h-2 rounded-full overflow-hidden flex">
                <div className="bg-emerald-500 h-full w-[24%]" />
                <div className="bg-slate-700 h-full w-[76%]" />
              </div>
            </div>

            <div className="bg-slate-900/80 border border-slate-800 rounded-xl p-4 space-y-2">
              <div className="flex items-center justify-between">
                <span className="text-xs text-slate-400 font-semibold">Security & Non-Root User</span>
                <ShieldCheck className="h-4 w-4 text-emerald-400" />
              </div>
              <p className="text-xs text-slate-300 font-mono">UID: 10001 (appuser:appgroup)</p>
              <p className="text-[11px] text-slate-400">
                Ngăn chặn container breakout attacks và hạn chế quyền truy cập host root filesystem.
              </p>
            </div>

            <div className="bg-slate-900/80 border border-slate-800 rounded-xl p-4 space-y-2">
              <div className="flex items-center justify-between">
                <span className="text-xs text-slate-400 font-semibold">Container Healthcheck</span>
                <Activity className="h-4 w-4 text-cyan-400" />
              </div>
              <p className="text-xs text-slate-300 font-mono">/actuator/health (Spring Boot)</p>
              <p className="text-[11px] text-slate-400">
                Interval: 30s • Timeout: 5s • Start-Period: 45s • Retries: 3
              </p>
            </div>
          </div>

          {/* Dockerfile Inspector */}
          <div className="bg-slate-900/80 border border-slate-800 rounded-xl p-5 space-y-4">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <div className="flex items-center gap-2">
                <Container className="h-5 w-5 text-indigo-400" />
                <h3 className="font-semibold text-white text-sm">
                  Production Multi-Stage Dockerfile (`microservices/order-service/Dockerfile`)
                </h3>
              </div>

              <div className="flex items-center gap-1.5 bg-slate-950 p-1 rounded-lg border border-slate-800 text-xs">
                <button
                  onClick={() => setDockerView('multistage')}
                  className={`px-3 py-1 rounded transition-all ${
                    dockerView === 'multistage' ? 'bg-indigo-600 text-white' : 'text-slate-400 hover:text-slate-200'
                  }`}
                >
                  Multi-Stage Dockerfile
                </button>
                <button
                  onClick={() => setDockerView('layers')}
                  className={`px-3 py-1 rounded transition-all ${
                    dockerView === 'layers' ? 'bg-indigo-600 text-white' : 'text-slate-400 hover:text-slate-200'
                  }`}
                >
                  Layer Caching Strategy
                </button>
                <button
                  onClick={() => setDockerView('jvm')}
                  className={`px-3 py-1 rounded transition-all ${
                    dockerView === 'jvm' ? 'bg-indigo-600 text-white' : 'text-slate-400 hover:text-slate-200'
                  }`}
                >
                  Container JVM Tuning
                </button>
              </div>
            </div>

            <div className="bg-slate-950 p-4 rounded-lg border border-slate-800 font-mono text-xs text-slate-300 overflow-x-auto">
              {dockerView === 'multistage' && (
                <pre>{`# ==============================================================================
# STAGE 1: Dependency Cache & Compilation
# ==============================================================================
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
WORKDIR /build

# Copy POMs only to cache Maven dependency layer
COPY pom.xml ./
COPY order-service/pom.xml ./order-service/
RUN mvn dependency:go-offline -B

# Copy source code and build lean executable JAR
COPY order-service/src ./order-service/src
RUN mvn clean package -pl order-service -am -DskipTests

# ==============================================================================
# STAGE 2: Lightweight, Secure Production Runner
# ==============================================================================
FROM eclipse-temurin:21-jre-alpine AS runner

# Create non-root system user & group (CIS Benchmark Security)
RUN addgroup -S appgroup -g 10001 && adduser -S appuser -u 10001 -G appgroup
WORKDIR /app

# Copy compiled artifact from builder stage
COPY --from=builder --chown=appuser:appgroup /build/order-service/target/*.jar app.jar
USER appuser:appgroup
EXPOSE 8081

# Production JVM Tuning for Containers
ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

# Docker Orchestration Healthcheck
HEALTHCHECK --interval=30s --timeout=5s --start-period=45s --retries=3 \\
  CMD wget -q --spider http://localhost:8081/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]`}</pre>
              )}

              {dockerView === 'layers' && (
                <div className="space-y-4 font-sans">
                  <h4 className="text-sm font-semibold text-white">Tại sao cần tách riêng Layer POM và Layer Source Code?</h4>
                  <div className="space-y-2 text-xs text-slate-300 leading-relaxed">
                    <p>
                      1. <strong className="text-emerald-400">Tăng tốc độ CI/CD Build:</strong> Lệnh <code className="font-mono text-blue-300">mvn dependency:go-offline</code> tải trước toàn bộ thư viện dependencies (Spring Boot, Kafka, Redis) và lưu vào Docker Cache.
                    </p>
                    <p>
                      2. <strong className="text-emerald-400">Cache Invalidation:</strong> Khi lập trình viên chỉ thay đổi code trong <code className="font-mono text-blue-300">src/</code>, Docker sẽ bỏ qua bước tải 500MB dependencies và chỉ biên dịch lại file Java, giảm thời gian build từ <strong>3 phút xuống 12 giây</strong>.
                    </p>
                  </div>
                </div>
              )}

              {dockerView === 'jvm' && (
                <div className="space-y-4 font-sans">
                  <h4 className="text-sm font-semibold text-white">JVM Container Resource Allocation Flags</h4>
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-3 text-xs">
                    <div className="bg-slate-900 p-3 rounded border border-slate-800 space-y-1">
                      <code className="font-mono text-blue-400">-XX:MaxRAMPercentage=75.0</code>
                      <p className="text-slate-400">Tự động tính toán Heap Memory tối đa = 75% Kubernetes memory limit của Pod, chừa 25% cho JVM Metaspace, Thread stacks và OS.</p>
                    </div>
                    <div className="bg-slate-900 p-3 rounded border border-slate-800 space-y-1">
                      <code className="font-mono text-emerald-400">-XX:+UseG1GC</code>
                      <p className="text-slate-400">Garbage Collector hiện đại tối ưu hóa độ trễ thấp (Low Pause Time) cho các API backend phục vụ hàng nghìn RPS.</p>
                    </div>
                    <div className="bg-slate-900 p-3 rounded border border-slate-800 space-y-1">
                      <code className="font-mono text-amber-400">-XX:+ExitOnOutOfMemoryError</code>
                      <p className="text-slate-400">Buộc JVM thoát ngay lập tức khi xảy ra lỗi OOM để Kubernetes Liveness Probe phát hiện và restart Pod sạch sẽ.</p>
                    </div>
                    <div className="bg-slate-900 p-3 rounded border border-slate-800 space-y-1">
                      <code className="font-mono text-purple-400">-Djava.security.egd=file:/dev/./urandom</code>
                      <p className="text-slate-400">Tăng tốc độ khởi động Spring Boot Container bằng nguồn sinh số ngẫu nhiên non-blocking entropy.</p>
                    </div>
                  </div>
                </div>
              )}
            </div>
          </div>
        </div>
      )}

      {/* SECTION 3: KUBERNETES CLUSTER & HPA AUTOSCALING */}
      {activeSection === 'k8s' && (
        <div className="space-y-6">
          {/* Interactive HPA Autoscaler Simulator */}
          <div className="bg-slate-900/80 border border-slate-800 rounded-xl p-5 space-y-5">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <div className="flex items-center gap-2.5">
                <div className="h-8 w-8 rounded-lg bg-cyan-500/10 border border-cyan-500/20 flex items-center justify-center text-cyan-400">
                  <Gauge className="h-4 w-4" />
                </div>
                <div>
                  <h3 className="font-semibold text-white text-sm">
                    Kubernetes Horizontal Pod Autoscaler (HPA) Live Simulator
                  </h3>
                  <p className="text-xs text-slate-400">
                    Kéo thanh trượt CPU Load để quan sát thuật toán HPA tự động Scale Up / Scale Down số lượng Pods
                  </p>
                </div>
              </div>

              <div className="flex items-center gap-3">
                <span className="text-xs text-slate-400">HPA Target: <strong className="text-cyan-400 font-mono">CPU 70%</strong></span>
                <span className="px-2.5 py-1 text-xs rounded bg-cyan-950/80 border border-cyan-800 text-cyan-300 font-mono">
                  Active Pods: {activeReplicas} / 12 Max
                </span>
              </div>
            </div>

            {/* Slider Control */}
            <div className="bg-slate-950 p-4 rounded-xl border border-slate-800 space-y-3">
              <div className="flex items-center justify-between text-xs">
                <span className="text-slate-300 flex items-center gap-2">
                  <Sliders className="h-4 w-4 text-cyan-400" />
                  Cluster CPU Utilization:
                </span>
                <span className={`font-mono font-bold text-sm ${
                  cpuLoad > 85 ? 'text-red-400 animate-pulse' :
                  cpuLoad > 70 ? 'text-amber-400' :
                  'text-emerald-400'
                }`}>
                  {cpuLoad}% CPU
                </span>
              </div>

              <input
                type="range"
                min="10"
                max="100"
                value={cpuLoad}
                onChange={(e) => setCpuLoad(Number(e.target.value))}
                className="w-full h-2 bg-slate-800 rounded-lg appearance-none cursor-pointer accent-cyan-500"
              />

              <div className="flex justify-between text-[10px] font-mono text-slate-500">
                <span>10% (Idle - 2 Pods)</span>
                <span>45% (Normal - 3 Pods)</span>
                <span>75% (Surge - 5 Pods)</span>
                <span>100% (Black Friday Spike - 12 Pods)</span>
              </div>
            </div>

            {/* Live Pod Topology Matrix */}
            <div className="space-y-2">
              <span className="text-xs font-semibold text-slate-300">Live Pods in Namespace `logistics-prod`:</span>
              <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-6 gap-3">
                {Array.from({ length: activeReplicas }).map((_, index) => (
                  <div
                    key={index}
                    className="bg-slate-950 p-3 rounded-lg border border-cyan-900/50 flex flex-col justify-between space-y-2 transition-all hover:border-cyan-500/50"
                  >
                    <div className="flex items-center justify-between">
                      <div className="h-2 w-2 rounded-full bg-emerald-400 animate-ping" />
                      <span className="text-[10px] font-mono text-cyan-400">Pod #{index + 1}</span>
                    </div>
                    <div>
                      <p className="text-[11px] font-semibold text-slate-200 truncate">order-service-{Math.random().toString(36).substring(2, 7)}</p>
                      <p className="text-[10px] font-mono text-slate-500">Status: Running (1/1)</p>
                    </div>
                    <div className="pt-1 border-t border-slate-800/80 flex items-center justify-between text-[9px] text-slate-400">
                      <span>Restarts: 0</span>
                      <span className="text-emerald-400">Ready</span>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>

          {/* K8s Sub-Tabs: Architecture Specs & Probes */}
          <div className="bg-slate-900/80 border border-slate-800 rounded-xl p-5 space-y-4">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <div className="flex items-center gap-2">
                <Cloud className="h-4 w-4 text-cyan-400" />
                <h3 className="font-semibold text-white text-sm">Kubernetes Manifests & Health Probes</h3>
              </div>

              <div className="flex items-center gap-1.5 bg-slate-950 p-1 rounded-lg border border-slate-800 text-xs">
                <button
                  onClick={() => setK8sTab('topology')}
                  className={`px-3 py-1 rounded transition-all ${
                    k8sTab === 'topology' ? 'bg-cyan-600 text-white' : 'text-slate-400 hover:text-slate-200'
                  }`}
                >
                  Deployment & Service YAML
                </button>
                <button
                  onClick={() => setK8sTab('probes')}
                  className={`px-3 py-1 rounded transition-all ${
                    k8sTab === 'probes' ? 'bg-cyan-600 text-white' : 'text-slate-400 hover:text-slate-200'
                  }`}
                >
                  Liveness / Readiness / Startup Probes
                </button>
                <button
                  onClick={() => setK8sTab('manifests')}
                  className={`px-3 py-1 rounded transition-all ${
                    k8sTab === 'manifests' ? 'bg-cyan-600 text-white' : 'text-slate-400 hover:text-slate-200'
                  }`}
                >
                  Ingress & HPA YAML
                </button>
              </div>
            </div>

            <div className="bg-slate-950 p-4 rounded-lg border border-slate-800 font-mono text-xs text-slate-300 overflow-x-auto max-h-96">
              {k8sTab === 'topology' && (
                <pre>{`apiVersion: apps/v1
kind: Deployment
metadata:
  name: order-service-deployment
  namespace: logistics-prod
spec:
  replicas: 3
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
  selector:
    matchLabels:
      app: order-service
  template:
    metadata:
      labels:
        app: order-service
    spec:
      containers:
        - name: order-service
          image: ghcr.io/logistics-org/order-service:v1.0.0
          ports:
            - containerPort: 8081
          resources:
            requests:
              cpu: "300m"
              memory: "512Mi"
            limits:
              cpu: "1000m"
              memory: "1024Mi"
          livenessProbe:
            httpGet: { path: /actuator/health/liveness, port: 8081 }
            initialDelaySeconds: 40
            periodSeconds: 15
          readinessProbe:
            httpGet: { path: /actuator/health/readiness, port: 8081 }
            initialDelaySeconds: 25
            periodSeconds: 10
---
apiVersion: v1
kind: Service
metadata:
  name: order-service
  namespace: logistics-prod
spec:
  type: ClusterIP
  ports:
    - port: 8081
      targetPort: 8081
  selector:
    app: order-service`}</pre>
              )}

              {k8sTab === 'probes' && (
                <div className="space-y-4 font-sans text-xs">
                  <h4 className="font-semibold text-white text-sm">3 Cấp Độ Kiểm Tra Sức Khỏe (Health Probes) Trong Kubernetes</h4>
                  <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
                    <div className="bg-slate-900 p-3 rounded-lg border border-slate-800 space-y-2">
                      <div className="flex items-center justify-between">
                        <span className="font-bold text-amber-400">1. Startup Probe</span>
                        <span className="text-[10px] font-mono text-slate-500">First 60s</span>
                      </div>
                      <p className="text-slate-300">
                        Cho phép Spring Boot ứng dụng thời gian khởi động, nạp JPA EntityManager và kết nối Kafka mà không bị Liveness Probe giết sớm.
                      </p>
                    </div>

                    <div className="bg-slate-900 p-3 rounded-lg border border-slate-800 space-y-2">
                      <div className="flex items-center justify-between">
                        <span className="font-bold text-emerald-400">2. Readiness Probe</span>
                        <span className="text-[10px] font-mono text-slate-500">Every 10s</span>
                      </div>
                      <p className="text-slate-300">
                        Kiểm tra xem Pod có sẵn sàng nhận traffic từ Kubernetes Service không. Nếu DB bị nghẽn tạm thời, Pod sẽ tự động ngắt khỏi Service load balancer.
                      </p>
                    </div>

                    <div className="bg-slate-900 p-3 rounded-lg border border-slate-800 space-y-2">
                      <div className="flex items-center justify-between">
                        <span className="font-bold text-cyan-400">3. Liveness Probe</span>
                        <span className="text-[10px] font-mono text-slate-500">Every 15s</span>
                      </div>
                      <p className="text-slate-300">
                        Phát hiện trạng thái Deadlock hoặc OutOfMemory. Khi endpoint trả về HTTP 500 hoặc timeout 3 lần liên tiếp, Kubernetes sẽ tự động restart Pod.
                      </p>
                    </div>
                  </div>
                </div>
              )}

              {k8sTab === 'manifests' && (
                <pre>{`apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: order-service-hpa
  namespace: logistics-prod
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: order-service-deployment
  minReplicas: 3
  maxReplicas: 12
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 70
---
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: logistics-ingress
  namespace: logistics-prod
  annotations:
    kubernetes.io/ingress.class: "nginx"
    nginx.ingress.kubernetes.io/limit-rps: "100"
    nginx.ingress.kubernetes.io/ssl-redirect: "true"
spec:
  rules:
    - host: api.logistics-enterprise.com
      http:
        paths:
          - path: /
            pathType: Prefix
            backend:
              service:
                name: api-gateway-service
                port: { number: 8000 }`}</pre>
              )}
            </div>
          </div>
        </div>
      )}

      {/* SECTION 4: CLI RUNBOOK CHEATSHEET */}
      {activeSection === 'commands' && (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div className="bg-slate-900/80 border border-slate-800 rounded-xl p-4 space-y-3">
            <h4 className="font-semibold text-white text-xs flex items-center gap-2">
              <Container className="h-4 w-4 text-indigo-400" />
              Docker CLI Production Commands
            </h4>
            <div className="space-y-2 text-xs font-mono text-slate-300">
              <div className="p-2.5 rounded bg-slate-950 border border-slate-800 flex items-center justify-between">
                <code>docker buildx build -t app:v1 --push .</code>
                <button onClick={() => copyToClipboard('docker buildx build -t app:v1 --push .', 'cmd1')} className="text-slate-500 hover:text-slate-300">
                  {copiedSnippet === 'cmd1' ? <Check className="h-3.5 w-3.5 text-emerald-400" /> : <Copy className="h-3.5 w-3.5" />}
                </button>
              </div>
              <div className="p-2.5 rounded bg-slate-950 border border-slate-800 flex items-center justify-between">
                <code>docker-compose up -d --scale order-service=3</code>
                <button onClick={() => copyToClipboard('docker-compose up -d --scale order-service=3', 'cmd2')} className="text-slate-500 hover:text-slate-300">
                  {copiedSnippet === 'cmd2' ? <Check className="h-3.5 w-3.5 text-emerald-400" /> : <Copy className="h-3.5 w-3.5" />}
                </button>
              </div>
              <div className="p-2.5 rounded bg-slate-950 border border-slate-800 flex items-center justify-between">
                <code>docker logs -f --tail=100 logistics-api-gateway</code>
                <button onClick={() => copyToClipboard('docker logs -f --tail=100 logistics-api-gateway', 'cmd3')} className="text-slate-500 hover:text-slate-300">
                  {copiedSnippet === 'cmd3' ? <Check className="h-3.5 w-3.5 text-emerald-400" /> : <Copy className="h-3.5 w-3.5" />}
                </button>
              </div>
            </div>
          </div>

          <div className="bg-slate-900/80 border border-slate-800 rounded-xl p-4 space-y-3">
            <h4 className="font-semibold text-white text-xs flex items-center gap-2">
              <Cloud className="h-4 w-4 text-cyan-400" />
              Kubernetes Kubectl Troubleshooting Commands
            </h4>
            <div className="space-y-2 text-xs font-mono text-slate-300">
              <div className="p-2.5 rounded bg-slate-950 border border-slate-800 flex items-center justify-between">
                <code>kubectl get pods -n logistics-prod -w</code>
                <button onClick={() => copyToClipboard('kubectl get pods -n logistics-prod -w', 'cmd4')} className="text-slate-500 hover:text-slate-300">
                  {copiedSnippet === 'cmd4' ? <Check className="h-3.5 w-3.5 text-emerald-400" /> : <Copy className="h-3.5 w-3.5" />}
                </button>
              </div>
              <div className="p-2.5 rounded bg-slate-950 border border-slate-800 flex items-center justify-between">
                <code>kubectl rollout status deploy/order-service-deployment</code>
                <button onClick={() => copyToClipboard('kubectl rollout status deploy/order-service-deployment', 'cmd5')} className="text-slate-500 hover:text-slate-300">
                  {copiedSnippet === 'cmd5' ? <Check className="h-3.5 w-3.5 text-emerald-400" /> : <Copy className="h-3.5 w-3.5" />}
                </button>
              </div>
              <div className="p-2.5 rounded bg-slate-950 border border-slate-800 flex items-center justify-between">
                <code>kubectl rollout undo deploy/order-service-deployment</code>
                <button onClick={() => copyToClipboard('kubectl rollout undo deploy/order-service-deployment', 'cmd6')} className="text-slate-500 hover:text-slate-300">
                  {copiedSnippet === 'cmd6' ? <Check className="h-3.5 w-3.5 text-emerald-400" /> : <Copy className="h-3.5 w-3.5" />}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
