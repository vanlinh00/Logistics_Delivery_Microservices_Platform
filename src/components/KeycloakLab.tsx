import React, { useState, useMemo } from 'react';
import {
  ShieldCheck,
  Key,
  Lock,
  Unlock,
  UserCheck,
  Users,
  Layers,
  ArrowRight,
  CheckCircle2,
  XCircle,
  AlertTriangle,
  Copy,
  Check,
  ExternalLink,
  Code,
  Terminal,
  RefreshCw,
  Cpu,
  Fingerprint,
  Zap,
  Radio,
  FileCode,
  Sliders,
  Send,
  Eye
} from 'lucide-react';

interface KeycloakUser {
  id: string;
  username: string;
  name: string;
  email: string;
  avatar: string;
  badgeColor: string;
  roles: string[];
  clientRoles: Record<string, string[]>;
  description: string;
}

const PRESET_USERS: KeycloakUser[] = [
  {
    id: 'user-001',
    username: 'admin',
    name: 'Quản Trị Viên Hệ Thống',
    email: 'admin@logistics.vn',
    avatar: '👨‍💼',
    badgeColor: 'bg-red-500/20 text-red-300 border-red-500/40',
    roles: ['ROLE_ADMIN', 'ROLE_DISPATCHER', 'ROLE_MERCHANT', 'ROLE_COURIER', 'ROLE_CUSTOMER'],
    clientRoles: {
      'order-service': ['order:read', 'order:write', 'order:delete', 'order:admin'],
      'tracking-service': ['tracking:admin', 'gps:override'],
      'pickup-fleet-service': ['fleet:dispatch', 'fleet:manage']
    },
    description: 'Quyền cao nhất (Full Access): Quản lý toàn bộ đơn hàng, điều phối bưu tá, hủy đơn và quản trị hệ thống.'
  },
  {
    id: 'user-002',
    username: 'courier_hung',
    name: 'Nguyễn Văn Hùng (Bưu tá Q1)',
    email: 'hung.shipper@logistics.vn',
    avatar: '🛵',
    badgeColor: 'bg-emerald-500/20 text-emerald-300 border-emerald-500/40',
    roles: ['ROLE_COURIER'],
    clientRoles: {
      'order-service': ['order:status_update', 'order:read'],
      'tracking-service': ['tracking:update_location', 'tracking:scan_barcode']
    },
    description: 'Tài xế / Bưu tá: Cập nhật trạng thái giao nhận (PICKED_UP, DELIVERED), quét barcode mã vận đơn và bắn tọa độ GPS.'
  },
  {
    id: 'user-003',
    username: 'merchant_long',
    name: 'Trần Long (Chủ Shop TechStore)',
    email: 'long.techstore@shopee.vn',
    avatar: '🏬',
    badgeColor: 'bg-blue-500/20 text-blue-300 border-blue-500/40',
    roles: ['ROLE_MERCHANT', 'ROLE_CUSTOMER'],
    clientRoles: {
      'order-service': ['order:create', 'order:read_own', 'pricing:calculate'],
      'tracking-service': ['tracking:view_own']
    },
    description: 'Chủ shop / Nhà bán hàng: Tạo đơn hàng loạt, yêu cầu tài xế lấy hàng tận nơi và xem báo cáo cước phí.'
  },
  {
    id: 'user-004',
    username: 'customer_linh',
    name: 'Nguyễn Văn Linh (Khách Hàng)',
    email: 'vanlinh20192019@gmail.com',
    avatar: '👤',
    badgeColor: 'bg-amber-500/20 text-amber-300 border-amber-500/40',
    roles: ['ROLE_CUSTOMER'],
    clientRoles: {
      'order-service': ['order:create', 'order:read_own'],
      'tracking-service': ['tracking:public_read']
    },
    description: 'Khách hàng cá nhân: Tạo đơn gửi lẻ, tra cứu hành trình vận đơn và tính cước phí ước tính.'
  }
];

interface ApiTestEndpoint {
  id: string;
  method: 'GET' | 'POST' | 'PUT' | 'DELETE';
  path: string;
  name: string;
  description: string;
  requiredRoles: string[];
  samplePayload?: string;
}

const API_ENDPOINTS: ApiTestEndpoint[] = [
  {
    id: 'create_order',
    method: 'POST',
    path: '/api/v1/orders',
    name: 'Tạo Đơn Hàng Mới',
    description: 'Tạo vận đơn, kiểm tra địa chỉ và tính phí giao hàng.',
    requiredRoles: ['ROLE_ADMIN', 'ROLE_MERCHANT', 'ROLE_CUSTOMER'],
    samplePayload: JSON.stringify(
      {
        recipientName: 'Trần Thu Hà',
        recipientPhone: '0912345678',
        recipientAddress: '25 Lê Lợi, Q1, TP.HCM',
        totalWeightKg: 2.5,
        declaredValue: 3500000
      },
      null,
      2
    )
  },
  {
    id: 'update_status',
    method: 'PUT',
    path: '/api/v1/orders/VNX-984210/status',
    name: 'Cập Nhật Trạng Thái Đơn (Giao / Nhận)',
    description: 'Bưu tá quét mã vạch xác nhận lấy hàng hoặc giao thành công.',
    requiredRoles: ['ROLE_ADMIN', 'ROLE_COURIER', 'ROLE_DISPATCHER'],
    samplePayload: JSON.stringify(
      {
        newStatus: 'DELIVERED',
        note: 'Giao hàng thành công cho khách, nhận đủ tiền COD 3,500,000 VND',
        driverLat: 10.7769,
        driverLng: 106.7009
      },
      null,
      2
    )
  },
  {
    id: 'dispatch_fleet',
    method: 'POST',
    path: '/api/v1/fleet/dispatch',
    name: 'Điều Phối Xe & Bưu Tá Khu Vực',
    description: 'Phân bổ 50 bưu kiện cho tài xế theo thuật toán Nearest Neighbor.',
    requiredRoles: ['ROLE_ADMIN', 'ROLE_DISPATCHER'],
    samplePayload: JSON.stringify(
      {
        hubId: 'HUB-TAN-BINH-01',
        courierId: 'courier_hung',
        parcelCount: 42
      },
      null,
      2
    )
  },
  {
    id: 'delete_order',
    method: 'DELETE',
    path: '/api/v1/orders/VNX-984210',
    name: 'Hủy & Thu Hồi Đơn Hàng (Admin Only)',
    description: 'Hủy hóa đơn vận chuyển và giải phóng tiền ký quỹ.',
    requiredRoles: ['ROLE_ADMIN']
  },
  {
    id: 'public_track',
    method: 'GET',
    path: '/api/v1/orders/track/VNX-984210',
    name: 'Tra Cứu Mã Vận Đơn Công Khai',
    description: 'Tra cứu thông tin lộ trình bưu phẩm (Không yêu cầu đăng nhập).',
    requiredRoles: [] // Public
  }
];

export const KeycloakLab: React.FC = () => {
  const [selectedUser, setSelectedUser] = useState<KeycloakUser>(PRESET_USERS[0]);
  const [activeTab, setActiveTab] = useState<'interactive_sandbox' | 'token_inspector' | 'flow_architecture' | 'code_blueprint'>('interactive_sandbox');
  const [selectedEndpoint, setSelectedEndpoint] = useState<ApiTestEndpoint>(API_ENDPOINTS[0]);
  const [executionLog, setExecutionLog] = useState<Array<{ id: string; time: string; user: string; method: string; path: string; status: number; allowed: boolean; details: string }>>([
    {
      id: 'log-0',
      time: '09:45:12',
      user: 'admin',
      method: 'POST',
      path: '/api/v1/orders',
      status: 201,
      allowed: true,
      details: 'Spring Security evaluated @PreAuthorize("hasAnyRole(...)") -> GRANTED.'
    }
  ]);
  const [isExecuting, setIsExecuting] = useState(false);
  const [copiedSection, setCopiedSection] = useState<string | null>(null);
  const [blueprintCodeTab, setBlueprintCodeTab] = useState<'security_config' | 'jwt_converter' | 'controller_rbac' | 'realm_json' | 'docker_compose'>('security_config');

  // Simulated JWT Generation based on active user
  const generatedJwt = useMemo(() => {
    const header = {
      alg: 'RS256',
      typ: 'JWT',
      kid: 'keycloak-logistics-rsa-2024-k1'
    };

    const now = Math.floor(Date.now() / 1000);
    const payload = {
      exp: now + 3600,
      iat: now,
      jti: 'urn:uuid:' + selectedUser.id + '-' + now,
      iss: 'http://localhost:8180/realms/logistics-realm',
      aud: ['account', 'logistics-api-gateway', 'order-service', 'tracking-service'],
      sub: 'kc-uuid-' + selectedUser.id,
      typ: 'Bearer',
      azp: 'logistics-frontend',
      session_state: 'sess-' + Math.random().toString(36).substring(7),
      acr: '1',
      'allowed-origins': ['http://localhost:3000', 'https://*.run.app'],
      realm_access: {
        roles: ['default-roles-logistics-realm', 'offline_access', 'uma_authorization', ...selectedUser.roles]
      },
      resource_access: {
        'order-service': {
          roles: selectedUser.clientRoles['order-service'] || []
        },
        'tracking-service': {
          roles: selectedUser.clientRoles['tracking-service'] || []
        },
        'pickup-fleet-service': {
          roles: selectedUser.clientRoles['pickup-fleet-service'] || []
        }
      },
      scope: 'openid email profile roles',
      sid: 'sid-' + Math.random().toString(36).substring(7),
      email_verified: true,
      name: selectedUser.name,
      preferred_username: selectedUser.username,
      email: selectedUser.email
    };

    const signature = 'c9A8_xKl91...[Keycloak_RS256_Signature_Verified_By_JWKS_Certs_Public_Key]...M28xP';

    return {
      header,
      payload,
      signature,
      rawEncoded: `${btoa(JSON.stringify(header))}.${btoa(JSON.stringify(payload))}.${btoa(signature)}`
    };
  }, [selectedUser]);

  // Execute Simulated API Call
  const handleExecuteApi = () => {
    setIsExecuting(true);
    setTimeout(() => {
      const isPublic = selectedEndpoint.requiredRoles.length === 0;
      const hasPermission = isPublic || selectedEndpoint.requiredRoles.some(r => selectedUser.roles.includes(r));
      const httpStatus = isPublic ? 200 : hasPermission ? (selectedEndpoint.method === 'POST' ? 201 : 200) : 403;

      let reason = '';
      if (isPublic) {
        reason = 'Public endpoint: Matched permitAll() in ResourceServerSecurityConfig.java';
      } else if (hasPermission) {
        const matchedRole = selectedEndpoint.requiredRoles.find(r => selectedUser.roles.includes(r));
        reason = `Authenticated as "${selectedUser.username}". Matched @PreAuthorize with role: ${matchedRole}`;
      } else {
        reason = `Access Denied: User "${selectedUser.username}" with roles [${selectedUser.roles.join(', ')}] lacks required roles: [${selectedEndpoint.requiredRoles.join(', ')}]`;
      }

      const newLog = {
        id: 'log-' + Date.now(),
        time: new Date().toLocaleTimeString(),
        user: selectedUser.username,
        method: selectedEndpoint.method,
        path: selectedEndpoint.path,
        status: httpStatus,
        allowed: hasPermission || isPublic,
        details: reason
      };

      setExecutionLog(prev => [newLog, ...prev.slice(0, 8)]);
      setIsExecuting(false);
    }, 350);
  };

  const handleCopy = (text: string, id: string) => {
    navigator.clipboard.writeText(text);
    setCopiedSection(id);
    setTimeout(() => setCopiedSection(null), 2000);
  };

  return (
    <div className="space-y-6">
      {/* Top Banner */}
      <div className="bg-gradient-to-r from-indigo-950/80 via-slate-900 to-purple-950/80 border border-indigo-500/30 rounded-2xl p-6 relative overflow-hidden shadow-2xl">
        <div className="absolute -right-10 -bottom-10 opacity-10 pointer-events-none">
          <ShieldCheck className="w-80 h-80 text-indigo-400" />
        </div>

        <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-6 relative z-10">
          <div className="space-y-2">
            <div className="flex items-center gap-3">
              <div className="p-2.5 bg-indigo-500/20 border border-indigo-500/40 rounded-xl text-indigo-300 shadow-inner">
                <ShieldCheck className="h-6 w-6 text-indigo-400 animate-pulse" />
              </div>
              <div>
                <div className="flex items-center gap-2">
                  <h2 className="text-xl font-bold text-white tracking-tight">Keycloak 24 IAM & OAuth2 / OIDC Lab</h2>
                  <span className="px-2.5 py-0.5 text-[10px] font-mono font-bold bg-indigo-500/20 text-indigo-300 border border-indigo-500/40 rounded-full">
                    REALM: logistics-realm
                  </span>
                  <span className="px-2 py-0.5 text-[10px] font-mono bg-emerald-500/20 text-emerald-400 border border-emerald-500/40 rounded-full flex items-center gap-1">
                    <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-ping"></span> Port 8180 (Ready)
                  </span>
                </div>
                <p className="text-slate-400 text-xs mt-0.5">
                  Kiến trúc Xác thực Tập trung OpenID Connect, Token Relay qua Spring Cloud Gateway, và Phân quyền RBAC (Role-Based Access Control) cho toàn bộ Microservices.
                </p>
              </div>
            </div>
          </div>

          <div className="flex items-center gap-2 flex-wrap">
            <div className="bg-slate-950/80 border border-slate-800 rounded-xl px-3 py-2 text-xs font-mono text-slate-300">
              <span className="text-slate-500">JWKS Certs:</span> <span className="text-indigo-300">/protocol/openid-connect/certs</span>
            </div>
            <a
              href="http://localhost:8180/admin/master/console/"
              target="_blank"
              rel="noreferrer"
              className="flex items-center gap-1.5 px-3.5 py-2 rounded-xl text-xs font-semibold bg-indigo-600 hover:bg-indigo-500 text-white shadow-lg shadow-indigo-600/30 transition-all"
            >
              <ExternalLink className="h-3.5 w-3.5" /> Keycloak Admin Console
            </a>
          </div>
        </div>

        {/* Navigation Tabs */}
        <div className="flex items-center gap-2 mt-6 border-t border-slate-800/80 pt-4 overflow-x-auto">
          {[
            { id: 'interactive_sandbox', label: '⚡ Interactive RBAC Sandbox', icon: Zap },
            { id: 'token_inspector', label: '🔍 Live JWT Inspector (Claims & Roles)', icon: Key },
            { id: 'flow_architecture', label: '🗺️ OIDC & Gateway Flow Diagram', icon: Layers },
            { id: 'code_blueprint', label: '💻 Spring Boot 3.4 & Security Blueprint', icon: FileCode }
          ].map(tab => {
            const Icon = tab.icon;
            return (
              <button
                key={tab.id}
                onClick={() => setActiveTab(tab.id as any)}
                className={`flex items-center gap-2 px-4 py-2 rounded-xl text-xs font-semibold whitespace-nowrap transition-all ${
                  activeTab === tab.id
                    ? 'bg-indigo-500 text-white shadow-lg shadow-indigo-500/25'
                    : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/60'
                }`}
              >
                <Icon className="h-3.5 w-3.5" />
                {tab.label}
              </button>
            );
          })}
        </div>
      </div>

      {/* USER PERSONA SWITCHER BAR */}
      <div className="bg-slate-900/90 border border-slate-800 rounded-2xl p-4 shadow-lg">
        <div className="flex items-center justify-between flex-wrap gap-3 mb-3">
          <div className="flex items-center gap-2">
            <Users className="h-4 w-4 text-indigo-400" />
            <span className="text-xs font-bold text-slate-200 uppercase tracking-wider">Chọn Người Dùng Keycloak Đang Đăng Nhập (Simulated Active Principal):</span>
          </div>
          <span className="text-[11px] text-slate-400 font-mono">
            Sub: <span className="text-slate-200">{selectedUser.id}</span> | Roles: <span className="text-amber-300 font-bold">{selectedUser.roles.join(', ')}</span>
          </span>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3">
          {PRESET_USERS.map(user => {
            const isSelected = selectedUser.id === user.id;
            return (
              <button
                key={user.id}
                onClick={() => setSelectedUser(user)}
                className={`p-3.5 rounded-xl border text-left transition-all relative overflow-hidden flex flex-col justify-between ${
                  isSelected
                    ? 'bg-indigo-950/60 border-indigo-500 shadow-md shadow-indigo-500/20 ring-1 ring-indigo-500/50'
                    : 'bg-slate-950/60 border-slate-800 hover:border-slate-700 hover:bg-slate-900/60'
                }`}
              >
                {isSelected && (
                  <div className="absolute top-2 right-2 flex items-center gap-1 text-[10px] font-bold text-emerald-400 bg-emerald-950/60 px-1.5 py-0.5 rounded border border-emerald-500/30">
                    <CheckCircle2 className="h-3 w-3" /> ACTIVE
                  </div>
                )}
                <div>
                  <div className="flex items-center gap-2.5 mb-1.5">
                    <span className="text-2xl p-1 bg-slate-900 rounded-lg border border-slate-800">{user.avatar}</span>
                    <div>
                      <div className="text-xs font-bold text-white flex items-center gap-1.5">
                        {user.username}
                      </div>
                      <div className="text-[11px] text-slate-400 truncate max-w-[150px]">{user.name}</div>
                    </div>
                  </div>
                  <p className="text-[11px] text-slate-400 line-clamp-2 mt-1">{user.description}</p>
                </div>

                <div className="mt-3 pt-2 border-t border-slate-800/80 flex flex-wrap gap-1">
                  {user.roles.map(r => (
                    <span key={r} className="px-1.5 py-0.5 text-[9px] font-mono font-bold bg-slate-900 text-indigo-300 rounded border border-indigo-500/30">
                      {r.replace('ROLE_', '')}
                    </span>
                  ))}
                </div>
              </button>
            );
          })}
        </div>
      </div>

      {/* TAB 1: INTERACTIVE RBAC SANDBOX */}
      {activeTab === 'interactive_sandbox' && (
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
          {/* Left: Endpoint Selector & Request Trigger (7 cols) */}
          <div className="lg:col-span-7 space-y-4">
            <div className="bg-slate-900/90 border border-slate-800 rounded-2xl p-5 space-y-4">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <Sliders className="h-4 w-4 text-indigo-400" />
                  <h3 className="text-sm font-bold text-white">Kiểm Thử Phân Quyền Endpoint (@PreAuthorize Sandbox)</h3>
                </div>
                <span className="text-[11px] text-slate-400 font-mono">Microservice: order-service (8081)</span>
              </div>

              {/* Endpoint selection buttons */}
              <div className="space-y-2">
                <label className="text-xs text-slate-400 font-medium">Chọn API Endpoint muốn gọi:</label>
                <div className="grid grid-cols-1 gap-2">
                  {API_ENDPOINTS.map(ep => {
                    const isSelected = selectedEndpoint.id === ep.id;
                    const isAllowed = ep.requiredRoles.length === 0 || ep.requiredRoles.some(r => selectedUser.roles.includes(r));
                    return (
                      <button
                        key={ep.id}
                        onClick={() => setSelectedEndpoint(ep)}
                        className={`p-3 rounded-xl border text-left transition-all flex items-center justify-between ${
                          isSelected
                            ? 'bg-slate-800 border-indigo-500 ring-1 ring-indigo-500/40'
                            : 'bg-slate-950/60 border-slate-800/80 hover:border-slate-700'
                        }`}
                      >
                        <div className="flex items-center gap-3">
                          <span
                            className={`px-2 py-1 rounded text-[10px] font-mono font-bold ${
                              ep.method === 'POST'
                                ? 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/30'
                                : ep.method === 'PUT'
                                ? 'bg-amber-500/20 text-amber-300 border border-amber-500/30'
                                : ep.method === 'DELETE'
                                ? 'bg-red-500/20 text-red-300 border border-red-500/30'
                                : 'bg-blue-500/20 text-blue-300 border border-blue-500/30'
                            }`}
                          >
                            {ep.method}
                          </span>
                          <div>
                            <div className="text-xs font-semibold text-white font-mono">{ep.path}</div>
                            <div className="text-[11px] text-slate-400">{ep.name}</div>
                          </div>
                        </div>

                        <div className="flex items-center gap-2">
                          {isAllowed ? (
                            <span className="flex items-center gap-1 text-[11px] text-emerald-400 font-semibold bg-emerald-950/50 px-2 py-0.5 rounded border border-emerald-500/30">
                              <CheckCircle2 className="h-3 w-3" /> Cho phép
                            </span>
                          ) : (
                            <span className="flex items-center gap-1 text-[11px] text-red-400 font-semibold bg-red-950/50 px-2 py-0.5 rounded border border-red-500/30">
                              <XCircle className="h-3 w-3" /> Cấm (403)
                            </span>
                          )}
                        </div>
                      </button>
                    );
                  })}
                </div>
              </div>

              {/* Endpoint Details & Requirements */}
              <div className="bg-slate-950 border border-slate-800 rounded-xl p-4 space-y-3 font-mono text-xs">
                <div className="flex items-center justify-between text-slate-400">
                  <span>Yêu cầu Role Spring Security:</span>
                  <span className="text-amber-300 font-bold">
                    {selectedEndpoint.requiredRoles.length === 0 ? 'permitAll() (Public)' : `@PreAuthorize("hasAnyRole('${selectedEndpoint.requiredRoles.join("', '")}')")`}
                  </span>
                </div>

                {selectedEndpoint.samplePayload && (
                  <div>
                    <span className="text-slate-400 text-[11px]">Request Body (JSON Payload):</span>
                    <pre className="mt-1 p-2.5 bg-slate-900 border border-slate-800 rounded-lg text-slate-300 text-[11px] overflow-x-auto max-h-32">
                      {selectedEndpoint.samplePayload}
                    </pre>
                  </div>
                )}

                <div className="pt-2 flex items-center justify-between">
                  <div className="text-[11px] text-slate-400 font-sans">
                    Gửi Bearer Token của <strong className="text-white">{selectedUser.username}</strong> qua API Gateway:
                  </div>
                  <button
                    onClick={handleExecuteApi}
                    disabled={isExecuting}
                    className="flex items-center gap-2 px-4 py-2 bg-indigo-600 hover:bg-indigo-500 text-white rounded-xl font-bold text-xs shadow-lg shadow-indigo-600/30 transition-all font-sans disabled:opacity-50"
                  >
                    {isExecuting ? <RefreshCw className="h-3.5 w-3.5 animate-spin" /> : <Send className="h-3.5 w-3.5" />}
                    {isExecuting ? 'Đang xác thực qua Keycloak...' : 'Gửi Request (Test API)'}
                  </button>
                </div>
              </div>
            </div>
          </div>

          {/* Right: Real-time Evaluation Audit & Access Decision (5 cols) */}
          <div className="lg:col-span-5 space-y-4">
            <div className="bg-slate-900/90 border border-slate-800 rounded-2xl p-5 flex flex-col h-full space-y-4">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <Terminal className="h-4 w-4 text-emerald-400" />
                  <h3 className="text-sm font-bold text-white">Security Evaluation Audit Trail</h3>
                </div>
                <button
                  onClick={() => setExecutionLog([])}
                  className="text-[10px] text-slate-400 hover:text-slate-200 underline font-mono"
                >
                  Xóa Log
                </button>
              </div>

              {/* Log stream */}
              <div className="flex-1 space-y-2.5 overflow-y-auto max-h-[480px]">
                {executionLog.length === 0 ? (
                  <div className="p-8 text-center text-slate-500 text-xs">
                    Chưa có log. Hãy bấm nút <strong>Gửi Request (Test API)</strong> để kiểm tra phân quyền.
                  </div>
                ) : (
                  executionLog.map(log => (
                    <div
                      key={log.id}
                      className={`p-3 rounded-xl border font-mono text-xs space-y-1.5 transition-all ${
                        log.allowed
                          ? 'bg-emerald-950/20 border-emerald-500/30 text-emerald-200'
                          : 'bg-red-950/20 border-red-500/30 text-red-200'
                      }`}
                    >
                      <div className="flex items-center justify-between text-[11px]">
                        <span className="text-slate-400">{log.time}</span>
                        <div className="flex items-center gap-1.5">
                          <span className="font-bold text-slate-300">User: {log.user}</span>
                          <span
                            className={`px-1.5 py-0.5 rounded text-[10px] font-bold ${
                              log.status >= 200 && log.status < 300
                                ? 'bg-emerald-500/30 text-emerald-300'
                                : 'bg-red-500/30 text-red-300'
                            }`}
                          >
                            HTTP {log.status} {log.status === 403 ? 'FORBIDDEN' : log.status === 201 ? 'CREATED' : 'OK'}
                          </span>
                        </div>
                      </div>

                      <div className="text-[11px] text-slate-300">
                        <strong className="text-white">{log.method}</strong> {log.path}
                      </div>

                      <div className="text-[10px] text-slate-400 bg-slate-950/60 p-1.5 rounded border border-slate-800">
                        {log.details}
                      </div>
                    </div>
                  ))
                )}
              </div>
            </div>
          </div>
        </div>
      )}

      {/* TAB 2: LIVE JWT TOKEN INSPECTOR */}
      {activeTab === 'token_inspector' && (
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
          {/* Encoded Token (Left 5 cols) */}
          <div className="lg:col-span-5 space-y-4">
            <div className="bg-slate-900/90 border border-slate-800 rounded-2xl p-5 space-y-3">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <Fingerprint className="h-4 w-4 text-indigo-400" />
                  <h3 className="text-sm font-bold text-white">Bearer Access Token (Encoded)</h3>
                </div>
                <button
                  onClick={() => handleCopy(generatedJwt.rawEncoded, 'encoded_jwt')}
                  className="flex items-center gap-1 text-xs text-indigo-400 hover:text-indigo-300 font-mono"
                >
                  {copiedSection === 'encoded_jwt' ? <Check className="h-3.5 w-3.5 text-emerald-400" /> : <Copy className="h-3.5 w-3.5" />}
                  {copiedSection === 'encoded_jwt' ? 'Copied' : 'Copy'}
                </button>
              </div>

              <div className="p-3 bg-slate-950 border border-slate-800 rounded-xl font-mono text-[11px] break-all leading-relaxed max-h-72 overflow-y-auto">
                <span className="text-red-400 font-bold">{btoa(JSON.stringify(generatedJwt.header))}</span>
                <span className="text-slate-500">.</span>
                <span className="text-purple-400 font-bold">{btoa(JSON.stringify(generatedJwt.payload))}</span>
                <span className="text-slate-500">.</span>
                <span className="text-blue-400 font-bold">{btoa(generatedJwt.signature)}</span>
              </div>

              <div className="space-y-1.5 text-xs text-slate-400 pt-2 border-t border-slate-800">
                <div className="flex items-center gap-2">
                  <span className="w-2.5 h-2.5 rounded-full bg-red-400"></span> <span className="font-mono text-red-300">Header:</span> Thuật toán ký RS256 & Key ID (kid)
                </div>
                <div className="flex items-center gap-2">
                  <span className="w-2.5 h-2.5 rounded-full bg-purple-400"></span> <span className="font-mono text-purple-300">Payload:</span> Claims, Roles, Identity & Scopes
                </div>
                <div className="flex items-center gap-2">
                  <span className="w-2.5 h-2.5 rounded-full bg-blue-400"></span> <span className="font-mono text-blue-300">Signature:</span> Chữ ký điện tử xác thực từ Keycloak JWKS
                </div>
              </div>
            </div>
          </div>

          {/* Decoded Claims (Right 7 cols) */}
          <div className="lg:col-span-7 space-y-4">
            <div className="bg-slate-900/90 border border-slate-800 rounded-2xl p-5 space-y-4">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <Eye className="h-4 w-4 text-emerald-400" />
                  <h3 className="text-sm font-bold text-white">Decoded Claims Payload (JSON)</h3>
                </div>
                <button
                  onClick={() => handleCopy(JSON.stringify(generatedJwt.payload, null, 2), 'payload_json')}
                  className="flex items-center gap-1 text-xs text-emerald-400 hover:text-emerald-300 font-mono"
                >
                  {copiedSection === 'payload_json' ? <Check className="h-3.5 w-3.5" /> : <Copy className="h-3.5 w-3.5" />}
                  {copiedSection === 'payload_json' ? 'Copied' : 'Copy JSON'}
                </button>
              </div>

              <div className="bg-slate-950 border border-slate-800 rounded-xl p-4 font-mono text-[11px] text-slate-300 max-h-96 overflow-y-auto leading-relaxed">
                <pre>{JSON.stringify(generatedJwt.payload, null, 2)}</pre>
              </div>

              {/* Key Highlights */}
              <div className="grid grid-cols-2 gap-3 font-sans text-xs">
                <div className="bg-indigo-950/30 border border-indigo-500/30 rounded-xl p-3">
                  <div className="text-indigo-400 font-bold text-xs mb-1">👑 Realm Access Roles:</div>
                  <div className="flex flex-wrap gap-1">
                    {selectedUser.roles.map(r => (
                      <span key={r} className="px-2 py-0.5 bg-indigo-900/60 text-indigo-200 rounded font-mono text-[10px] font-bold">
                        {r}
                      </span>
                    ))}
                  </div>
                </div>

                <div className="bg-purple-950/30 border border-purple-500/30 rounded-xl p-3">
                  <div className="text-purple-400 font-bold text-xs mb-1">📦 Client Resource Roles:</div>
                  <div className="text-[11px] font-mono text-slate-300">
                    order-service: [{selectedUser.clientRoles['order-service']?.join(', ') || 'none'}]
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* TAB 3: OIDC & GATEWAY FLOW ARCHITECTURE */}
      {activeTab === 'flow_architecture' && (
        <div className="bg-slate-900/90 border border-slate-800 rounded-2xl p-6 space-y-6">
          <div className="flex items-center justify-between flex-wrap gap-2">
            <div>
              <h3 className="text-base font-bold text-white">Quy Trình Xác Thực & Ủy Quyền Keycloak trong Microservices</h3>
              <p className="text-slate-400 text-xs mt-0.5">
                Mô hình Authorization Code Flow + PKCE cho Client SPA và Token Relay qua Spring Cloud API Gateway tới các Resource Server.
              </p>
            </div>
            <span className="px-3 py-1 bg-indigo-500/20 border border-indigo-500/40 text-indigo-300 rounded-xl text-xs font-mono font-bold">
              Standard: OAuth2.1 / OIDC Core 1.0
            </span>
          </div>

          {/* Interactive Steps Grid */}
          <div className="grid grid-cols-1 md:grid-cols-4 gap-4 relative">
            {/* Step 1 */}
            <div className="bg-slate-950 border border-slate-800 rounded-xl p-4 space-y-2 relative">
              <div className="flex items-center justify-between">
                <span className="w-6 h-6 rounded-full bg-indigo-600 text-white flex items-center justify-center text-xs font-bold">1</span>
                <span className="text-[10px] font-mono text-slate-500">React Frontend</span>
              </div>
              <h4 className="text-xs font-bold text-white">OIDC Login & PKCE</h4>
              <p className="text-[11px] text-slate-400 leading-relaxed">
                Người dùng mở trình duyệt, ứng dụng React chuyển hướng sang Keycloak Login Page với <code className="text-indigo-300">code_challenge</code> để bảo vệ chống đánh cắp token.
              </p>
            </div>

            {/* Step 2 */}
            <div className="bg-slate-950 border border-slate-800 rounded-xl p-4 space-y-2 relative">
              <div className="flex items-center justify-between">
                <span className="w-6 h-6 rounded-full bg-indigo-600 text-white flex items-center justify-center text-xs font-bold">2</span>
                <span className="text-[10px] font-mono text-indigo-400 font-bold">Keycloak Server</span>
              </div>
              <h4 className="text-xs font-bold text-white">Cấp Phát RS256 JWT</h4>
              <p className="text-[11px] text-slate-400 leading-relaxed">
                Keycloak kiểm tra thông tin đăng nhập trong PostgreSQL DB, sinh Access Token (RS256 JWT) kèm theo Realm Roles & Client Scopes.
              </p>
            </div>

            {/* Step 3 */}
            <div className="bg-slate-950 border border-slate-800 rounded-xl p-4 space-y-2 relative">
              <div className="flex items-center justify-between">
                <span className="w-6 h-6 rounded-full bg-indigo-600 text-white flex items-center justify-center text-xs font-bold">3</span>
                <span className="text-[10px] font-mono text-amber-400 font-bold">Spring Cloud Gateway</span>
              </div>
              <h4 className="text-xs font-bold text-white">Token Relay Filter</h4>
              <p className="text-[11px] text-slate-400 leading-relaxed">
                Gateway chặn request, kiểm tra Access Token hợp lệ và chuyển tiếp header <code className="text-amber-300">Authorization: Bearer</code> xuống các Microservice nội bộ.
              </p>
            </div>

            {/* Step 4 */}
            <div className="bg-slate-950 border border-slate-800 rounded-xl p-4 space-y-2 relative">
              <div className="flex items-center justify-between">
                <span className="w-6 h-6 rounded-full bg-indigo-600 text-white flex items-center justify-center text-xs font-bold">4</span>
                <span className="text-[10px] font-mono text-emerald-400 font-bold">Order / Fleet Service</span>
              </div>
              <h4 className="text-xs font-bold text-white">JWKS Check & RBAC</h4>
              <p className="text-[11px] text-slate-400 leading-relaxed">
                Service dùng public certs từ <code className="text-emerald-300">/protocol/openid-connect/certs</code> để xác thực token và thực thi <code className="text-emerald-300">@PreAuthorize</code>.
              </p>
            </div>
          </div>

          {/* Architecture comparison box */}
          <div className="bg-indigo-950/20 border border-indigo-500/30 rounded-xl p-4 flex items-start gap-3">
            <Zap className="h-5 w-5 text-indigo-400 shrink-0 mt-0.5" />
            <div className="space-y-1 text-xs text-slate-300">
              <h4 className="font-bold text-indigo-300">Tại sao chọn Keycloak cho hệ thống Logistics Microservices?</h4>
              <p className="text-slate-400 leading-relaxed">
                1. <strong>Không chia sẻ Secret Key (Zero Shared Secrets):</strong> Sử dụng cơ chế Public/Private Key (RS256). Các Microservice chỉ cần lấy Public Certs của Keycloak một lần và cache lại, không bao giờ lo rò rỉ JWT Secret.<br />
                2. <strong>Single Sign-On (SSO):</strong> Bưu tá, Chủ shop và Admin chỉ cần đăng nhập 1 lần là có thể truy cập Web Portal, Mobile App và Dispatch Dashboard.<br />
                3. <strong>Quản trị tập trung (Centralized IAM):</strong> Khóa tài khoản, reset mật khẩu, ép đổi pass 2FA (TOTP) và phân quyền vai trò tức thì mà không cần chạm vào code Java.
              </p>
            </div>
          </div>
        </div>
      )}

      {/* TAB 4: CODE BLUEPRINT & SPRING BOOT 3.4 CONFIG */}
      {activeTab === 'code_blueprint' && (
        <div className="bg-slate-900/90 border border-slate-800 rounded-2xl overflow-hidden shadow-2xl flex flex-col h-[650px]">
          {/* Header Subtabs */}
          <div className="bg-slate-950 border-b border-slate-800 p-2.5 flex items-center justify-between flex-wrap gap-2">
            <div className="flex items-center gap-1.5 overflow-x-auto">
              {[
                { id: 'security_config', label: 'ResourceServerSecurityConfig.java' },
                { id: 'jwt_converter', label: 'KeycloakJwtAuthenticationConverter.java' },
                { id: 'controller_rbac', label: 'OrderController.java (@PreAuthorize)' },
                { id: 'realm_json', label: 'logistics-realm.json (Keycloak Realm)' },
                { id: 'docker_compose', label: 'docker-compose.yml (Keycloak Service)' }
              ].map(tab => (
                <button
                  key={tab.id}
                  onClick={() => setBlueprintCodeTab(tab.id as any)}
                  className={`px-3 py-1.5 rounded-lg text-xs font-mono font-medium transition-all ${
                    blueprintCodeTab === tab.id
                      ? 'bg-indigo-600 text-white font-bold shadow'
                      : 'text-slate-400 hover:text-white hover:bg-slate-800'
                  }`}
                >
                  {tab.label}
                </button>
              ))}
            </div>

            <button
              onClick={() => {
                let code = '';
                if (blueprintCodeTab === 'security_config') code = springSecuritySnippet;
                if (blueprintCodeTab === 'jwt_converter') code = keycloakConverterSnippet;
                if (blueprintCodeTab === 'controller_rbac') code = controllerRbacSnippet;
                if (blueprintCodeTab === 'realm_json') code = realmJsonSnippet;
                if (blueprintCodeTab === 'docker_compose') code = dockerComposeSnippet;
                handleCopy(code, 'blueprint_code');
              }}
              className="flex items-center gap-1.5 px-3 py-1 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs font-mono shrink-0"
            >
              {copiedSection === 'blueprint_code' ? <Check className="h-3.5 w-3.5 text-emerald-400" /> : <Copy className="h-3.5 w-3.5" />}
              {copiedSection === 'blueprint_code' ? 'Copied Code' : 'Copy Snippet'}
            </button>
          </div>

          {/* Code View */}
          <div className="flex-1 p-4 bg-slate-950 overflow-y-auto font-mono text-[11px] leading-relaxed text-indigo-200">
            {blueprintCodeTab === 'security_config' && <pre className="whitespace-pre-wrap">{springSecuritySnippet}</pre>}
            {blueprintCodeTab === 'jwt_converter' && <pre className="whitespace-pre-wrap">{keycloakConverterSnippet}</pre>}
            {blueprintCodeTab === 'controller_rbac' && <pre className="whitespace-pre-wrap">{controllerRbacSnippet}</pre>}
            {blueprintCodeTab === 'realm_json' && <pre className="whitespace-pre-wrap">{realmJsonSnippet}</pre>}
            {blueprintCodeTab === 'docker_compose' && <pre className="whitespace-pre-wrap">{dockerComposeSnippet}</pre>}
          </div>
        </div>
      )}
    </div>
  );
};

// Code Snippets for Inspector
const springSecuritySnippet = `@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true)
@RequiredArgsConstructor
public class ResourceServerSecurityConfig {

    private final KeycloakJwtAuthenticationConverter keycloakJwtAuthenticationConverter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // 1. Public endpoints
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/actuator/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/orders/calculate-price").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/orders/track/**").permitAll()
                // 2. Secured business routes (Evaluated by Keycloak RS256 JWT)
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(keycloakJwtAuthenticationConverter))
            );

        return http.build();
    }
}`;

const keycloakConverterSnippet = `@Component
public class KeycloakJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(@NonNull Jwt jwt) {
        Collection<GrantedAuthority> authorities = extractKeycloakRoles(jwt);
        String username = jwt.getClaimAsString("preferred_username");
        return new JwtAuthenticationToken(jwt, authorities, username);
    }

    private Collection<GrantedAuthority> extractKeycloakRoles(Jwt jwt) {
        Set<GrantedAuthority> authorities = new HashSet<>();

        // 1. Extract Realm Roles (e.g. ROLE_ADMIN, ROLE_COURIER, ROLE_MERCHANT)
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess != null && realmAccess.containsKey("roles")) {
            List<String> roles = (List<String>) realmAccess.get("roles");
            roles.forEach(role -> {
                String roleName = role.startsWith("ROLE_") ? role : "ROLE_" + role;
                authorities.add(new SimpleGrantedAuthority(roleName));
            });
        }

        return authorities;
    }
}`;

const controllerRbacSnippet = `@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "Order Management", description = "Protected by Keycloak OAuth2 / OIDC")
public class OrderController {

    // 1. Khách hàng và Merchant được tạo đơn
    @PostMapping
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_MERCHANT', 'ROLE_CUSTOMER')")
    public ResponseEntity<ApiResponse<Order>> createOrder(@Valid @RequestBody OrderDTOs.CreateOrderRequest request) {
        return new ResponseEntity<>(ApiResponse.created(orderService.createOrder(request)), HttpStatus.CREATED);
    }

    // 2. Bưu tá & Điều phối được cập nhật trạng thái đơn
    @PutMapping("/{orderId}/status")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_COURIER', 'ROLE_DISPATCHER')")
    public ResponseEntity<ApiResponse<Order>> updateStatus(
            @PathVariable UUID orderId,
            @Valid @RequestBody OrderDTOs.UpdateOrderStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(orderService.updateOrderStatus(orderId, request)));
    }

    // 3. Chỉ Quản Trị Viên (Admin) được phép hủy và xóa đơn hàng
    @DeleteMapping("/{orderId}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteOrder(@PathVariable UUID orderId) {
        return ResponseEntity.ok(ApiResponse.ok(null, "Order canceled and voided"));
    }
}`;

const realmJsonSnippet = `{
  "realm": "logistics-realm",
  "displayName": "Logistics & Delivery Enterprise IAM",
  "enabled": true,
  "roles": {
    "realm": [
      { "name": "ROLE_ADMIN", "description": "Full Access Logistics Administrator" },
      { "name": "ROLE_COURIER", "description": "Delivery Shipper / Driver" },
      { "name": "ROLE_MERCHANT", "description": "E-commerce Shop Owner" },
      { "name": "ROLE_DISPATCHER", "description": "Fleet Hub Coordinator" },
      { "name": "ROLE_CUSTOMER", "description": "End Consumer" }
    ]
  },
  "clients": [
    {
      "clientId": "logistics-frontend",
      "publicClient": true,
      "standardFlowEnabled": true,
      "redirectUris": ["http://localhost:3000/*"],
      "webOrigins": ["*"]
    },
    {
      "clientId": "logistics-api-gateway",
      "secret": "logistics-gateway-secret-2024-enterprise-jwt",
      "bearerOnly": false
    },
    {
      "clientId": "order-service",
      "bearerOnly": true
    }
  ]
}`;

const dockerComposeSnippet = `  keycloak:
    image: quay.io/keycloak/keycloak:24.0.2
    container_name: logistics-keycloak
    command: start-dev --import-realm
    ports:
      - "8180:8080"
    environment:
      - KEYCLOAK_ADMIN=admin
      - KEYCLOAK_ADMIN_PASSWORD=admin
      - KC_DB=postgres
      - KC_DB_URL=jdbc:postgresql://postgres:5432/keycloak_db
      - KC_DB_USERNAME=postgres
      - KC_DB_PASSWORD=postgres
      - KC_HEALTH_ENABLED=true
    volumes:
      - ./infrastructure/keycloak/logistics-realm.json:/opt/keycloak/data/import/logistics-realm.json:ro
    depends_on:
      - postgres
    networks:
      - logistics-network`;
