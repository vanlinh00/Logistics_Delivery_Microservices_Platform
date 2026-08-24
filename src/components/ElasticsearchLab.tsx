import React, { useState, useMemo } from 'react';
import {
  Search,
  Database,
  Sliders,
  MapPin,
  Code2,
  Activity,
  CheckCircle2,
  Clock,
  Truck,
  Package,
  Layers,
  Sparkles,
  Zap,
  Filter,
  Copy,
  Check,
  RefreshCw,
  Eye,
  Crosshair,
  TrendingUp,
  Tag,
  AlertCircle,
  ExternalLink,
  ChevronRight,
  ShieldCheck,
  Cpu
} from 'lucide-react';

interface ParcelDocument {
  id: string;
  trackingNumber: string;
  senderName: string;
  senderPhone: string;
  senderAddress: string;
  originCity: string;
  recipientName: string;
  recipientPhone: string;
  recipientAddress: string;
  destinationCity: string;
  destinationDistrict: string;
  currentStatus: 'PICKUP_SCHEDULED' | 'IN_TRANSIT' | 'OUT_FOR_DELIVERY' | 'DELIVERED' | 'RETURNED';
  statusDescription: string;
  currentHub: string;
  lat: number;
  lon: number;
  weightKg: number;
  shippingFee: number;
  codAmount: number;
  shippingServiceType: 'STANDARD' | 'EXPRESS' | 'HEAVY_FREIGHT' | 'COLD_CHAIN';
  assignedCarrierName: string;
  assignedCarrierPhone: string;
  itemDescription: string;
  tags: string[];
  lastUpdated: string;
}

const SAMPLE_ES_PARCELS: ParcelDocument[] = [
  {
    id: 'ORD-984210',
    trackingNumber: 'ORD-984210',
    senderName: 'Phạm Hoàng Long - TechStore Cầu Giấy',
    senderPhone: '0903112233',
    senderAddress: 'Số 18 Duy Tân, Dịch Vọng Hậu, Cầu Giấy',
    originCity: 'Hà Nội',
    recipientName: 'Nguyễn Văn Linh',
    recipientPhone: '0984210001',
    recipientAddress: 'Tòa nhà Bitexco, Số 2 Hải Triều, Bến Nghé, Quận 1',
    destinationCity: 'TP. Hồ Chí Minh',
    destinationDistrict: 'Quận 1',
    currentStatus: 'IN_TRANSIT',
    statusDescription: 'Đang trung chuyển qua Hub Tân Bình Express, chuẩn bị giao cho Shipper',
    currentHub: 'Hub Tân Bình Express Hub',
    lat: 10.8015,
    lon: 106.6644,
    weightKg: 1.85,
    shippingFee: 45000,
    codAmount: 1450000,
    shippingServiceType: 'EXPRESS',
    assignedCarrierName: 'Nguyễn Văn Hùng (Courier-01)',
    assignedCarrierPhone: '0912345678',
    itemDescription: 'Laptop Dell XPS 15 9530 Core i7, Chuột Logitech MX Master 3S, Sạc 130W',
    tags: ['HIGH_VALUE', 'FRAGILE', 'ELECTRONICS'],
    lastUpdated: '5 phút trước'
  },
  {
    id: 'ORD-984211',
    trackingNumber: 'ORD-984211',
    senderName: 'Công ty TNHH Thời Trang May Mặc An Phú',
    senderPhone: '0933445566',
    senderAddress: 'Khu Công Nghiệp Tân Bình, Tây Thạnh',
    originCity: 'TP. Hồ Chí Minh',
    recipientName: 'Trần Thị Mai Phương',
    recipientPhone: '0977889900',
    recipientAddress: 'Số 45 Lê Duẩn, Phường Thạch Thang, Quận Hải Châu',
    destinationCity: 'Đà Nẵng',
    destinationDistrict: 'Hải Châu',
    currentStatus: 'OUT_FOR_DELIVERY',
    statusDescription: 'Shipper đang mang hàng đi giao cho khách, liên hệ trước khi tới',
    currentHub: 'Bưu cục Hải Châu - Đà Nẵng',
    lat: 16.0680,
    lon: 108.2120,
    weightKg: 0.75,
    shippingFee: 32000,
    codAmount: 520000,
    shippingServiceType: 'STANDARD',
    assignedCarrierName: 'Lê Văn Tùng (Courier-04)',
    assignedCarrierPhone: '0945678901',
    itemDescription: 'Váy lụa tơ tằm thiết kế cao cấp, Áo Blazer màu be dáng rộng',
    tags: ['FASHION', 'COD'],
    lastUpdated: '12 phút trước'
  },
  {
    id: 'ORD-984212',
    trackingNumber: 'ORD-984212',
    senderName: 'Tổng Kho Cảng Hải Phòng Logistics Depot',
    senderPhone: '0918223344',
    senderAddress: 'Đường Chùa Vẽ, Phường Đông Hải 1, Quận Hải An',
    originCity: 'Hải Phòng',
    recipientName: 'Samsung Electronics Vietnam Complex',
    recipientPhone: '0966554433',
    recipientAddress: 'KCN Tiên Sơn, Phường Đồng Nguyên, TP. Từ Sơn',
    destinationCity: 'Bắc Ninh',
    destinationDistrict: 'Từ Sơn',
    currentStatus: 'PICKUP_SCHEDULED',
    statusDescription: 'Đã phân bổ xe tải 5 tấn nhận lô hàng linh kiện điện tử',
    currentHub: 'Kho Tiên Sơn Logistics Hub',
    lat: 21.1215,
    lon: 105.9750,
    weightKg: 45.0,
    shippingFee: 380000,
    codAmount: 0,
    shippingServiceType: 'HEAVY_FREIGHT',
    assignedCarrierName: 'Trần Đình Trọng (Fleet-Truck-09)',
    assignedCarrierPhone: '0981122334',
    itemDescription: 'Linh kiện bán dẫn vi xử lý, Bảng mạch PCB cao tầng, Cảm biến quang học',
    tags: ['HEAVY_FREIGHT', 'B2B', 'INDUSTRIAL'],
    lastUpdated: '25 phút trước'
  },
  {
    id: 'ORD-984213',
    trackingNumber: 'ORD-984213',
    senderName: 'Dược Phẩm Sinh Học MedPharma VN',
    senderPhone: '0908889999',
    senderAddress: 'Khu Công Nghệ Cao, Long Thạnh Mỹ, TP. Thủ Đức',
    originCity: 'TP. Hồ Chí Minh',
    recipientName: 'Bệnh viện Đa Khoa Trung Ương Cần Thơ',
    recipientPhone: '0907776655',
    recipientAddress: 'Số 315 Nguyễn Văn Linh, Phường An Khánh, Quận Ninh Kiều',
    destinationCity: 'Cần Thơ',
    destinationDistrict: 'Ninh Kiều',
    currentStatus: 'IN_TRANSIT',
    statusDescription: 'Thùng hàng kiểm soát nhiệt độ nghiêm ngặt 2°C - 8°C trên xe lạnh',
    currentHub: 'Trạm trung chuyển Tiền Giang',
    lat: 10.3600,
    lon: 106.3600,
    weightKg: 8.2,
    shippingFee: 250000,
    codAmount: 0,
    shippingServiceType: 'COLD_CHAIN',
    assignedCarrierName: 'Võ Minh Trí (ColdFleet-02)',
    assignedCarrierPhone: '0934567812',
    itemDescription: 'Vắc xin sinh học bảo quản lạnh, Huyết thanh kháng độc, Thuốc đặc trị chuyên khoa',
    tags: ['COLD_CHAIN', 'MEDICAL', 'URGENT'],
    lastUpdated: '30 phút trước'
  },
  {
    id: 'ORD-984214',
    trackingNumber: 'ORD-984214',
    senderName: 'Văn Phòng Phẩm & Sách Nhã Nam',
    senderPhone: '0912334455',
    senderAddress: '59 Đỗ Quang, Trung Hòa, Cầu Giấy',
    originCity: 'Hà Nội',
    recipientName: 'Hoàng Nhật Minh',
    recipientPhone: '0988776655',
    recipientAddress: 'Số 12 Ngõ 198 Kim Mã, Giảng Võ, Ba Đình',
    destinationCity: 'Hà Nội',
    destinationDistrict: 'Ba Đình',
    currentStatus: 'DELIVERED',
    statusDescription: 'Giao hàng thành công lúc 14:30. Ký nhận bởi Hoàng Nhật Minh.',
    currentHub: 'Bưu cục Ba Đình Express',
    lat: 21.0333,
    lon: 105.8190,
    weightKg: 1.2,
    shippingFee: 22000,
    codAmount: 180000,
    shippingServiceType: 'STANDARD',
    assignedCarrierName: 'Đỗ Anh Dũng (Courier-07)',
    assignedCarrierPhone: '0923456789',
    itemDescription: 'Bộ sách Lịch Sử Văn Minh Thế Giới (5 tập bìa cứng), Sổ tay da cao cấp',
    tags: ['BOOKS', 'DELIVERED'],
    lastUpdated: '1 giờ trước'
  },
  {
    id: 'ORD-984215',
    trackingNumber: 'ORD-984215',
    senderName: 'Apple Flagship Store Diamond Plaza',
    senderPhone: '0901234567',
    senderAddress: '34 Lê Duẩn, Bến Nghé, Quận 1',
    originCity: 'TP. Hồ Chí Minh',
    recipientName: 'Vũ Hải Đăng',
    recipientPhone: '0933221100',
    recipientAddress: 'Landmark 81, 720A Điện Biên Phủ, Phường 22, Bình Thạnh',
    destinationCity: 'TP. Hồ Chí Minh',
    destinationDistrict: 'Bình Thạnh',
    currentStatus: 'OUT_FOR_DELIVERY',
    statusDescription: 'Shipper VIP đang trên đường giao hàng siêu tốc trong 2h',
    currentHub: 'Hub Bình Thạnh Express',
    lat: 10.7950,
    lon: 106.7218,
    weightKg: 0.95,
    shippingFee: 60000,
    codAmount: 32990000,
    shippingServiceType: 'EXPRESS',
    assignedCarrierName: 'Bùi Gia Huy (Express-05)',
    assignedCarrierPhone: '0909123888',
    itemDescription: 'iPhone 16 Pro Max 256GB Titan Tự Nhiên, Ốp lưng MagSafe chính hãng',
    tags: ['HIGH_VALUE', 'EXPRESS_2H', 'COD_LARGE'],
    lastUpdated: '8 phút trước'
  }
];

export const ElasticsearchLab: React.FC = () => {
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedStatus, setSelectedStatus] = useState<string>('ALL');
  const [selectedCity, setSelectedCity] = useState<string>('ALL');
  const [selectedServiceType, setSelectedServiceType] = useState<string>('ALL');
  const [activeDslTab, setActiveDslTab] = useState<'search' | 'autocomplete' | 'geo' | 'aggs' | 'mapping' | 'java'>('search');
  const [copiedCode, setCopiedCode] = useState(false);

  // Geo Spatial Radar State
  const [selectedHub, setSelectedHub] = useState<'hcm' | 'hn' | 'dn'>('hcm');
  const [geoRadiusKm, setGeoRadiusKm] = useState<number>(20);
  const [isGeoFilterActive, setIsGeoFilterActive] = useState<boolean>(false);

  // Autocomplete dropdown
  const [showSuggestions, setShowSuggestions] = useState(false);

  // Live Sync Simulator State
  const [syncStatus, setSyncStatus] = useState<string | null>(null);

  const hubs = {
    hcm: { name: 'TP.HCM Tân Bình Hub', lat: 10.8015, lon: 106.6644, city: 'TP. Hồ Chí Minh' },
    hn: { name: 'Hà Nội Cầu Giấy Hub', lat: 21.0285, lon: 105.8542, city: 'Hà Nội' },
    dn: { name: 'Đà Nẵng Hải Châu Hub', lat: 16.0680, lon: 108.2120, city: 'Đà Nẵng' }
  };

  // Helper calculate distance
  const calculateDistance = (lat1: number, lon1: number, lat2: number, lon2: number) => {
    const R = 6371; // Earth radius in km
    const dLat = ((lat2 - lat1) * Math.PI) / 180;
    const dLon = ((lon2 - lon1) * Math.PI) / 180;
    const a =
      Math.sin(dLat / 2) * Math.sin(dLat / 2) +
      Math.cos((lat1 * Math.PI) / 180) * Math.cos((lat2 * Math.PI) / 180) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return R * c;
  };

  // Calculate matching score and fuzzy search simulation
  const searchResults = useMemo(() => {
    const q = searchQuery.toLowerCase().trim();

    return SAMPLE_ES_PARCELS.filter((p) => {
      // 1. Text & Fuzzy filter
      let textMatch = true;
      if (q) {
        // Strip common prefixes or diacritics
        const searchableText = `${p.trackingNumber} ${p.recipientName} ${p.recipientPhone} ${p.senderName} ${p.itemDescription} ${p.recipientAddress} ${p.destinationCity} ${p.currentHub} ${p.tags.join(' ')}`.toLowerCase();
        
        // Exact / prefix / fuzzy contains
        const keywords = q.split(' ').filter(Boolean);
        textMatch = keywords.every((kw) => {
          if (searchableText.includes(kw)) return true;
          // Simple fuzzy: allow 1-2 character transposition/typo
          if (kw.length >= 4) {
            const sub = kw.substring(0, kw.length - 1);
            if (searchableText.includes(sub)) return true;
          }
          return false;
        });
      }

      // 2. Status filter
      let statusMatch = true;
      if (selectedStatus !== 'ALL') {
        statusMatch = p.currentStatus === selectedStatus;
      }

      // 3. City filter
      let cityMatch = true;
      if (selectedCity !== 'ALL') {
        cityMatch = p.destinationCity === selectedCity;
      }

      // 4. Service Type filter
      let serviceMatch = true;
      if (selectedServiceType !== 'ALL') {
        serviceMatch = p.shippingServiceType === selectedServiceType;
      }

      // 5. Geo-Spatial Filter
      let geoMatch = true;
      if (isGeoFilterActive) {
        const currentHubInfo = hubs[selectedHub];
        const dist = calculateDistance(currentHubInfo.lat, currentHubInfo.lon, p.lat, p.lon);
        geoMatch = dist <= geoRadiusKm;
      }

      return textMatch && statusMatch && cityMatch && serviceMatch && geoMatch;
    }).map((item, idx) => {
      // Calculate simulated Elasticsearch relevance _score
      let score = 1.0;
      if (q) {
        if (item.trackingNumber.toLowerCase().includes(q)) score += 8.5;
        if (item.recipientPhone.includes(q)) score += 6.0;
        if (item.recipientName.toLowerCase().includes(q)) score += 4.2;
        if (item.itemDescription.toLowerCase().includes(q)) score += 3.8;
      } else {
        score = 1.0 + (SAMPLE_ES_PARCELS.length - idx) * 0.15;
      }
      return { ...item, esScore: score.toFixed(2) };
    }).sort((a, b) => parseFloat(b.esScore) - parseFloat(a.esScore));
  }, [searchQuery, selectedStatus, selectedCity, selectedServiceType, isGeoFilterActive, selectedHub, geoRadiusKm]);

  // Autocomplete Suggestions list
  const suggestions = useMemo(() => {
    if (!searchQuery || searchQuery.length < 2) return [];
    const q = searchQuery.toLowerCase();
    const list: { text: string; type: string; tracking: string }[] = [];
    SAMPLE_ES_PARCELS.forEach((p) => {
      if (p.trackingNumber.toLowerCase().includes(q)) {
        list.push({ text: p.trackingNumber, type: 'TRACKING_CODE', tracking: p.trackingNumber });
      }
      if (p.recipientName.toLowerCase().includes(q)) {
        list.push({ text: p.recipientName, type: 'RECIPIENT_NAME', tracking: p.trackingNumber });
      }
      if (p.itemDescription.toLowerCase().includes(q)) {
        list.push({ text: p.itemDescription.split(',')[0], type: 'ITEM', tracking: p.trackingNumber });
      }
    });
    return list.slice(0, 5);
  }, [searchQuery]);

  // Facet Counts
  const statusFacets = useMemo(() => {
    const counts: Record<string, number> = { ALL: SAMPLE_ES_PARCELS.length };
    SAMPLE_ES_PARCELS.forEach((p) => {
      counts[p.currentStatus] = (counts[p.currentStatus] || 0) + 1;
    });
    return counts;
  }, []);

  const cityFacets = useMemo(() => {
    const counts: Record<string, number> = { ALL: SAMPLE_ES_PARCELS.length };
    SAMPLE_ES_PARCELS.forEach((p) => {
      counts[p.destinationCity] = (counts[p.destinationCity] || 0) + 1;
    });
    return counts;
  }, []);

  // Generated Live Query DSL
  const currentGeneratedDsl = useMemo(() => {
    const mustClauses: any[] = [];
    const filterClauses: any[] = [];

    if (searchQuery.trim()) {
      mustClauses.push({
        multi_match: {
          query: searchQuery,
          fields: [
            'trackingNumber^8',
            'recipientPhone^5',
            'recipientName^4',
            'itemDescription^3',
            'recipientAddress^2',
            'senderName',
            'currentHub'
          ],
          fuzziness: 'AUTO',
          prefix_length: 1
        }
      });
    } else {
      mustClauses.push({ match_all: {} });
    }

    if (selectedStatus !== 'ALL') {
      filterClauses.push({ term: { currentStatus: selectedStatus } });
    }
    if (selectedCity !== 'ALL') {
      filterClauses.push({ term: { destinationCity: selectedCity } });
    }
    if (selectedServiceType !== 'ALL') {
      filterClauses.push({ term: { shippingServiceType: selectedServiceType } });
    }
    if (isGeoFilterActive) {
      const hub = hubs[selectedHub];
      filterClauses.push({
        geo_distance: {
          distance: `${geoRadiusKm}km`,
          currentLocation: {
            lat: hub.lat,
            lon: hub.lon
          }
        }
      });
    }

    return JSON.stringify(
      {
        query: {
          bool: {
            must: mustClauses,
            ...(filterClauses.length > 0 ? { filter: filterClauses } : {})
          }
        },
        highlight: {
          fields: {
            recipientName: {},
            itemDescription: {},
            recipientAddress: {},
            trackingNumber: {}
          }
        },
        aggs: {
          status_breakdown: { terms: { field: 'currentStatus' } },
          destination_cities: { terms: { field: 'destinationCity' } },
          avg_shipping_fee: { stats: { field: 'shippingFee' } }
        },
        sort: [{ _score: { order: 'desc' } }, { lastUpdatedAt: { order: 'desc' } }],
        size: 20
      },
      null,
      2
    );
  }, [searchQuery, selectedStatus, selectedCity, selectedServiceType, isGeoFilterActive, selectedHub, geoRadiusKm]);

  const handleCopyCode = (text: string) => {
    navigator.clipboard.writeText(text);
    setCopiedCode(true);
    setTimeout(() => setCopiedCode(false), 2000);
  };

  const triggerKafkaCdcSync = () => {
    setSyncStatus('Ingesting Kafka event `logistics.tracking.event-recorded` -> Syncing Elasticsearch...');
    setTimeout(() => {
      setSyncStatus('Index updated! Document `ORD-984210` re-scored with 0 lag.');
      setTimeout(() => setSyncStatus(null), 4000);
    }, 900);
  };

  return (
    <div className="space-y-6" id="elasticsearch-lab-container">
      {/* Header Banner */}
      <div className="bg-gradient-to-r from-slate-900 via-amber-950/30 to-slate-900 border border-amber-500/30 rounded-2xl p-6 relative overflow-hidden">
        <div className="absolute right-0 top-0 w-96 h-96 bg-amber-500/10 rounded-full blur-3xl pointer-events-none" />
        <div className="flex flex-col md:flex-row items-start md:items-center justify-between gap-4 relative z-10">
          <div>
            <div className="flex items-center gap-2 mb-2">
              <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold bg-amber-500/15 text-amber-300 border border-amber-500/30">
                <Database className="h-3.5 w-3.5" />
                Elasticsearch 8.12 Distributed Cluster
              </span>
              <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-medium bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                <CheckCircle2 className="h-3 w-3" />
                Cluster Status: GREEN
              </span>
            </div>
            <h2 className="text-2xl font-bold text-white tracking-tight">
              Logistics Distributed Search & Analytics Engine
            </h2>
            <p className="text-slate-400 text-sm mt-1 max-w-2xl">
              High-throughput full-text fuzzy querying, Edge N-gram autocomplete, spatial Geo-Distance radius filtering, and real-time Kafka Change Data Capture (CDC) syncing.
            </p>
          </div>

          <div className="flex items-center gap-2">
            <button
              onClick={triggerKafkaCdcSync}
              className="flex items-center gap-2 px-3.5 py-2 rounded-xl bg-amber-500/20 hover:bg-amber-500/30 border border-amber-500/40 text-amber-200 text-xs font-semibold transition-all shadow-sm active:scale-95"
            >
              <Zap className="h-4 w-4 text-amber-400 animate-pulse" />
              Simulate Kafka CDC Ingestion
            </button>
          </div>
        </div>

        {/* Sync notification */}
        {syncStatus && (
          <div className="mt-4 p-3 rounded-xl bg-amber-500/20 border border-amber-500/40 flex items-center gap-3 text-amber-200 text-xs animate-fade-in">
            <RefreshCw className="h-4 w-4 animate-spin text-amber-400" />
            <span className="font-mono">{syncStatus}</span>
          </div>
        )}
      </div>

      {/* Cluster Telemetry Stats */}
      <div className="grid grid-cols-2 md:grid-cols-5 gap-3">
        <div className="bg-slate-900/60 border border-slate-800 rounded-xl p-3.5">
          <p className="text-xs text-slate-400">Indexed Parcels</p>
          <p className="text-lg font-bold text-white mt-0.5">142,850 docs</p>
          <span className="text-[10px] text-emerald-400 flex items-center gap-1 mt-1">
            <TrendingUp className="h-3 w-3" /> +1,240 / hr Kafka CDC
          </span>
        </div>
        <div className="bg-slate-900/60 border border-slate-800 rounded-xl p-3.5">
          <p className="text-xs text-slate-400">P95 Search Latency</p>
          <p className="text-lg font-bold text-amber-300 mt-0.5">2.4 ms</p>
          <span className="text-[10px] text-slate-400 mt-1 block">In-memory Lucene cache</span>
        </div>
        <div className="bg-slate-900/60 border border-slate-800 rounded-xl p-3.5">
          <p className="text-xs text-slate-400">Active Shards</p>
          <p className="text-lg font-bold text-white mt-0.5">3 Primary / 3 Replica</p>
          <span className="text-[10px] text-emerald-400 mt-1 block">100% Allocated</span>
        </div>
        <div className="bg-slate-900/60 border border-slate-800 rounded-xl p-3.5">
          <p className="text-xs text-slate-400">JVM Heap Allocation</p>
          <p className="text-lg font-bold text-white mt-0.5">412 MB / 1024 MB</p>
          <span className="text-[10px] text-blue-400 mt-1 block">G1GC 40.2% Usage</span>
        </div>
        <div className="bg-slate-900/60 border border-slate-800 rounded-xl p-3.5 col-span-2 md:col-span-1">
          <p className="text-xs text-slate-400">Index Storage Size</p>
          <p className="text-lg font-bold text-white mt-0.5">54.8 MB</p>
          <span className="text-[10px] text-purple-400 mt-1 block">logistics_parcels_v1</span>
        </div>
      </div>

      {/* Main Search Experience & DSL Split */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        {/* Left Column: Interactive Search Console (7 cols) */}
        <div className="lg:col-span-7 space-y-4">
          {/* Main Search Input & Presets */}
          <div className="bg-slate-900/70 border border-slate-800 rounded-2xl p-4 space-y-3">
            <div className="relative">
              <Search className="absolute left-3.5 top-3.5 h-4 w-4 text-slate-400" />
              <input
                type="text"
                value={searchQuery}
                onChange={(e) => {
                  setSearchQuery(e.target.value);
                  setShowSuggestions(true);
                }}
                onFocus={() => setShowSuggestions(true)}
                placeholder="Search by Tracking Code, Consignee Phone, Item, Recipient Name, or Typo (e.g. 'laptap', 'ORD-984', '0984210', 'Hanoi')..."
                className="w-full bg-slate-950 border border-slate-700/80 rounded-xl pl-10 pr-10 py-2.5 text-sm text-white placeholder-slate-500 focus:outline-none focus:border-amber-500/80 focus:ring-1 focus:ring-amber-500 transition-all font-mono"
              />
              {searchQuery && (
                <button
                  onClick={() => setSearchQuery('')}
                  className="absolute right-3 top-3 text-slate-500 hover:text-slate-300 text-xs px-1.5 py-0.5 rounded bg-slate-800"
                >
                  Clear
                </button>
              )}

              {/* Edge N-Gram Autocomplete Dropdown */}
              {showSuggestions && suggestions.length > 0 && (
                <div className="absolute left-0 right-0 top-full mt-1.5 bg-slate-950 border border-amber-500/40 rounded-xl shadow-2xl z-30 overflow-hidden">
                  <div className="px-3 py-1.5 bg-amber-500/10 border-b border-amber-500/20 flex items-center justify-between text-[11px] text-amber-300 font-semibold">
                    <span className="flex items-center gap-1">
                      <Sparkles className="h-3 w-3" /> Edge N-Gram Autocomplete Suggestions
                    </span>
                    <span className="font-mono text-slate-400">Took 0.8ms</span>
                  </div>
                  <div className="divide-y divide-slate-900">
                    {suggestions.map((s, idx) => (
                      <button
                        key={idx}
                        onClick={() => {
                          setSearchQuery(s.text);
                          setShowSuggestions(false);
                        }}
                        className="w-full px-3.5 py-2 text-left hover:bg-amber-500/10 flex items-center justify-between transition-colors"
                      >
                        <div className="flex items-center gap-2">
                          <span className="text-xs text-white font-medium">{s.text}</span>
                          <span className="text-[10px] px-1.5 py-0.5 rounded bg-slate-800 text-slate-400 font-mono">
                            {s.type}
                          </span>
                        </div>
                        <span className="text-[11px] text-slate-500 font-mono">{s.tracking}</span>
                      </button>
                    ))}
                  </div>
                </div>
              )}
            </div>

            {/* Query Presets Chips */}
            <div className="flex items-center gap-1.5 flex-wrap pt-1">
              <span className="text-[11px] text-slate-400 flex items-center gap-1 mr-1">
                <Sparkles className="h-3 w-3 text-amber-400" /> Fast Queries:
              </span>
              {[
                { label: 'ORD-984210', q: 'ORD-984210', desc: 'Exact ID' },
                { label: '0984210001', q: '0984210001', desc: 'Phone' },
                { label: 'Laptop Dell', q: 'Laptop Dell', desc: 'Item' },
                { label: 'laptap xps (Typo)', q: 'laptap', desc: 'Fuzzy Match' },
                { label: 'TP. Hồ Chí Minh', q: 'Hồ Chí Minh', desc: 'City' },
                { label: 'Vắc xin y tế', q: 'Vắc xin', desc: 'Cold Chain' }
              ].map((p, idx) => (
                <button
                  key={idx}
                  onClick={() => setSearchQuery(p.q)}
                  className="px-2.5 py-1 rounded-lg text-xs font-mono bg-slate-800/80 hover:bg-slate-700 hover:text-amber-300 text-slate-300 border border-slate-700/60 transition-all flex items-center gap-1"
                >
                  <span>{p.label}</span>
                </button>
              ))}
            </div>

            {/* Filter Pills */}
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-2.5 pt-2 border-t border-slate-800">
              {/* Status Filter */}
              <div>
                <label className="block text-[11px] font-medium text-slate-400 mb-1">Status Facet</label>
                <select
                  value={selectedStatus}
                  onChange={(e) => setSelectedStatus(e.target.value)}
                  className="w-full bg-slate-950 border border-slate-800 rounded-lg px-2.5 py-1.5 text-xs text-white focus:outline-none focus:border-amber-500"
                >
                  <option value="ALL">All Statuses ({statusFacets.ALL})</option>
                  <option value="IN_TRANSIT">IN_TRANSIT ({statusFacets.IN_TRANSIT || 0})</option>
                  <option value="OUT_FOR_DELIVERY">OUT_FOR_DELIVERY ({statusFacets.OUT_FOR_DELIVERY || 0})</option>
                  <option value="PICKUP_SCHEDULED">PICKUP_SCHEDULED ({statusFacets.PICKUP_SCHEDULED || 0})</option>
                  <option value="DELIVERED">DELIVERED ({statusFacets.DELIVERED || 0})</option>
                </select>
              </div>

              {/* City Filter */}
              <div>
                <label className="block text-[11px] font-medium text-slate-400 mb-1">Destination City</label>
                <select
                  value={selectedCity}
                  onChange={(e) => setSelectedCity(e.target.value)}
                  className="w-full bg-slate-950 border border-slate-800 rounded-lg px-2.5 py-1.5 text-xs text-white focus:outline-none focus:border-amber-500"
                >
                  <option value="ALL">All Cities ({cityFacets.ALL})</option>
                  <option value="TP. Hồ Chí Minh">TP. Hồ Chí Minh ({cityFacets['TP. Hồ Chí Minh'] || 0})</option>
                  <option value="Hà Nội">Hà Nội ({cityFacets['Hà Nội'] || 0})</option>
                  <option value="Đà Nẵng">Đà Nẵng ({cityFacets['Đà Nẵng'] || 0})</option>
                  <option value="Bắc Ninh">Bắc Ninh ({cityFacets['Bắc Ninh'] || 0})</option>
                  <option value="Cần Thơ">Cần Thơ ({cityFacets['Cần Thơ'] || 0})</option>
                </select>
              </div>

              {/* Service Type Filter */}
              <div>
                <label className="block text-[11px] font-medium text-slate-400 mb-1">Shipping Tier</label>
                <select
                  value={selectedServiceType}
                  onChange={(e) => setSelectedServiceType(e.target.value)}
                  className="w-full bg-slate-950 border border-slate-800 rounded-lg px-2.5 py-1.5 text-xs text-white focus:outline-none focus:border-amber-500"
                >
                  <option value="ALL">All Tiers</option>
                  <option value="EXPRESS">EXPRESS</option>
                  <option value="STANDARD">STANDARD</option>
                  <option value="HEAVY_FREIGHT">HEAVY_FREIGHT</option>
                  <option value="COLD_CHAIN">COLD_CHAIN</option>
                </select>
              </div>
            </div>

            {/* Spatial Geo-Distance Radar Control */}
            <div className="bg-slate-950/80 border border-slate-800/90 rounded-xl p-3 space-y-2.5">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <Crosshair className={`h-4 w-4 ${isGeoFilterActive ? 'text-amber-400 animate-spin' : 'text-slate-400'}`} />
                  <span className="text-xs font-semibold text-white">Spatial Geo-Distance Filter</span>
                </div>
                <label className="relative inline-flex items-center cursor-pointer">
                  <input
                    type="checkbox"
                    checked={isGeoFilterActive}
                    onChange={(e) => setIsGeoFilterActive(e.target.checked)}
                    className="sr-only peer"
                  />
                  <div className="w-9 h-5 bg-slate-800 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-4 after:w-4 after:transition-all peer-checked:bg-amber-500"></div>
                </label>
              </div>

              {isGeoFilterActive && (
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 pt-2 border-t border-slate-800/80 animate-fade-in">
                  <div>
                    <span className="text-[11px] text-slate-400">Reference Hub Location</span>
                    <div className="flex gap-1.5 mt-1">
                      {(['hcm', 'hn', 'dn'] as const).map((h) => (
                        <button
                          key={h}
                          onClick={() => setSelectedHub(h)}
                          className={`px-2.5 py-1 rounded text-xs font-medium transition-all ${
                            selectedHub === h
                              ? 'bg-amber-500 text-slate-950 font-semibold'
                              : 'bg-slate-800 text-slate-300 hover:bg-slate-700'
                          }`}
                        >
                          {hubs[h].city}
                        </button>
                      ))}
                    </div>
                  </div>

                  <div>
                    <div className="flex justify-between text-[11px] text-slate-400">
                      <span>Radius Perimeter:</span>
                      <span className="font-mono font-bold text-amber-300">{geoRadiusKm} km</span>
                    </div>
                    <input
                      type="range"
                      min="5"
                      max="100"
                      step="5"
                      value={geoRadiusKm}
                      onChange={(e) => setGeoRadiusKm(Number(e.target.value))}
                      className="w-full accent-amber-500 mt-2"
                    />
                  </div>
                </div>
              )}
            </div>
          </div>

          {/* Search Hits Results */}
          <div className="space-y-3">
            <div className="flex items-center justify-between px-1">
              <span className="text-xs font-semibold text-slate-300 flex items-center gap-1.5">
                <Package className="h-4 w-4 text-amber-400" />
                Elasticsearch Hits ({searchResults.length} results)
              </span>
              <span className="text-[11px] text-slate-400 font-mono">
                Query execution: <span className="text-emerald-400 font-semibold">1.8 ms</span>
              </span>
            </div>

            {searchResults.length === 0 ? (
              <div className="bg-slate-900/40 border border-slate-800 rounded-2xl p-8 text-center space-y-2">
                <AlertCircle className="h-8 w-8 text-amber-400/60 mx-auto" />
                <p className="text-sm font-semibold text-white">No parcel documents matched query</p>
                <p className="text-xs text-slate-400 max-w-sm mx-auto">
                  Try clearing some filters, increasing the geo radius, or searching with broader keywords.
                </p>
                <button
                  onClick={() => {
                    setSearchQuery('');
                    setSelectedStatus('ALL');
                    setSelectedCity('ALL');
                    setIsGeoFilterActive(false);
                  }}
                  className="mt-2 px-3 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-xs text-white"
                >
                  Reset All Filters
                </button>
              </div>
            ) : (
              searchResults.map((item) => (
                <div
                  key={item.id}
                  className="bg-slate-900/60 hover:bg-slate-900/90 border border-slate-800 hover:border-amber-500/40 rounded-xl p-4 transition-all space-y-3 group"
                >
                  {/* Top Bar: Code, Status, Score */}
                  <div className="flex items-center justify-between flex-wrap gap-2">
                    <div className="flex items-center gap-2">
                      <span className="font-mono text-sm font-bold text-white group-hover:text-amber-300 transition-colors">
                        {item.trackingNumber}
                      </span>
                      <span
                        className={`text-[10px] px-2 py-0.5 rounded font-semibold border ${
                          item.currentStatus === 'DELIVERED'
                            ? 'bg-emerald-500/10 text-emerald-300 border-emerald-500/30'
                            : item.currentStatus === 'OUT_FOR_DELIVERY'
                            ? 'bg-blue-500/10 text-blue-300 border-blue-500/30'
                            : item.currentStatus === 'IN_TRANSIT'
                            ? 'bg-amber-500/10 text-amber-300 border-amber-500/30'
                            : 'bg-purple-500/10 text-purple-300 border-purple-500/30'
                        }`}
                      >
                        {item.currentStatus}
                      </span>
                      <span className="text-[10px] px-2 py-0.5 rounded bg-slate-800 text-slate-400 font-mono">
                        {item.shippingServiceType}
                      </span>
                    </div>

                    <div className="flex items-center gap-2">
                      <span className="text-[11px] px-2 py-0.5 rounded bg-amber-500/10 text-amber-300 border border-amber-500/20 font-mono font-bold">
                        _score: {item.esScore}
                      </span>
                      <span className="text-[11px] text-slate-500 flex items-center gap-1">
                        <Clock className="h-3 w-3" /> {item.lastUpdated}
                      </span>
                    </div>
                  </div>

                  {/* Item Description & Highlights */}
                  <div className="bg-slate-950/70 border border-slate-800/80 rounded-lg p-2.5 text-xs text-slate-300 space-y-1">
                    <div className="flex items-center gap-1.5 text-slate-400 font-medium">
                      <Tag className="h-3.5 w-3.5 text-amber-400" />
                      <span>Hàng hóa:</span>
                    </div>
                    <p className="text-white font-medium pl-5">{item.itemDescription}</p>
                    <p className="text-[11px] text-slate-400 pl-5 flex items-center gap-1">
                      <Activity className="h-3 w-3 text-amber-400" /> {item.statusDescription}
                    </p>
                  </div>

                  {/* Route & Consignee */}
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 text-xs">
                    <div className="space-y-0.5">
                      <span className="text-[11px] text-slate-400">Người gửi:</span>
                      <p className="text-slate-200 font-medium truncate">{item.senderName}</p>
                      <p className="text-[11px] text-slate-500">{item.originCity}</p>
                    </div>
                    <div className="space-y-0.5">
                      <span className="text-[11px] text-slate-400">Người nhận & Địa chỉ:</span>
                      <p className="text-slate-200 font-medium">
                        {item.recipientName} • <span className="font-mono text-slate-400">{item.recipientPhone}</span>
                      </p>
                      <p className="text-[11px] text-slate-400 truncate">{item.recipientAddress}</p>
                    </div>
                  </div>

                  {/* Footer Stats & Tags */}
                  <div className="pt-2 border-t border-slate-800/80 flex items-center justify-between flex-wrap gap-2 text-[11px] text-slate-400">
                    <div className="flex items-center gap-1.5 flex-wrap">
                      {item.tags.map((t, tidx) => (
                        <span key={tidx} className="px-1.5 py-0.5 rounded bg-slate-800 text-slate-300 font-mono text-[10px]">
                          #{t}
                        </span>
                      ))}
                    </div>

                    <div className="flex items-center gap-3 font-mono">
                      <span>KL: <strong className="text-white">{item.weightKg} kg</strong></span>
                      <span>Cước: <strong className="text-white">{item.shippingFee.toLocaleString('vi-VN')} đ</strong></span>
                      {item.codAmount > 0 && (
                        <span>COD: <strong className="text-amber-300">{item.codAmount.toLocaleString('vi-VN')} đ</strong></span>
                      )}
                    </div>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>

        {/* Right Column: Elasticsearch Query DSL Inspector & Architecture (5 cols) */}
        <div className="lg:col-span-5 space-y-4">
          {/* Query DSL Live Viewer Box */}
          <div className="bg-slate-900/80 border border-slate-800 rounded-2xl overflow-hidden flex flex-col h-[680px]">
            {/* DSL Header Tabs */}
            <div className="bg-slate-950 border-b border-slate-800 p-2 flex items-center justify-between flex-wrap gap-1">
              <div className="flex items-center gap-1">
                {[
                  { id: 'search', label: 'Bool Query DSL' },
                  { id: 'autocomplete', label: 'Edge N-Gram' },
                  { id: 'geo', label: 'Geo Distance' },
                  { id: 'aggs', label: 'Aggs' },
                  { id: 'java', label: 'Java 21 Code' }
                ].map((t) => (
                  <button
                    key={t.id}
                    onClick={() => setActiveDslTab(t.id as any)}
                    className={`px-2.5 py-1 rounded-lg text-xs font-mono transition-all ${
                      activeDslTab === t.id
                        ? 'bg-amber-500 text-slate-950 font-bold shadow'
                        : 'text-slate-400 hover:text-white hover:bg-slate-800'
                    }`}
                  >
                    {t.label}
                  </button>
                ))}
              </div>

              <button
                onClick={() => {
                  let textToCopy = currentGeneratedDsl;
                  if (activeDslTab === 'autocomplete') textToCopy = autocompleteDslExample;
                  if (activeDslTab === 'geo') textToCopy = geoDslExample;
                  if (activeDslTab === 'aggs') textToCopy = aggsDslExample;
                  if (activeDslTab === 'java') textToCopy = springDataJavaCode;
                  handleCopyCode(textToCopy);
                }}
                className="flex items-center gap-1 px-2.5 py-1 rounded bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs font-mono"
              >
                {copiedCode ? <Check className="h-3 w-3 text-emerald-400" /> : <Copy className="h-3 w-3" />}
                {copiedCode ? 'Copied' : 'Copy'}
              </button>
            </div>

            {/* Code Body */}
            <div className="flex-1 p-4 bg-slate-950 overflow-y-auto font-mono text-[11px] leading-relaxed text-slate-300">
              {activeDslTab === 'search' && (
                <pre className="text-amber-300/90 whitespace-pre-wrap">{currentGeneratedDsl}</pre>
              )}

              {activeDslTab === 'autocomplete' && (
                <div className="space-y-3">
                  <p className="text-slate-400 text-xs font-sans">
                    Edge N-Gram Tokenizer index-time analysis generates character slices (min: 2, max: 15) allowing instant prefix search on tracking numbers, names, and phone numbers without wildcard latency overhead.
                  </p>
                  <pre className="text-amber-300 whitespace-pre-wrap">{autocompleteDslExample}</pre>
                </div>
              )}

              {activeDslTab === 'geo' && (
                <div className="space-y-3">
                  <p className="text-slate-400 text-xs font-sans">
                    Spatial Geo-Distance filter evaluates geo_point coordinates using the Haversine formula inside Elasticsearch's Lucene spatial index.
                  </p>
                  <pre className="text-cyan-300 whitespace-pre-wrap">{geoDslExample}</pre>
                </div>
              )}

              {activeDslTab === 'aggs' && (
                <div className="space-y-3">
                  <p className="text-slate-400 text-xs font-sans">
                    Elasticsearch Multi-Bucketing & Metric Aggregations execute across millions of parcels in milliseconds to compute status breakdowns and price metrics.
                  </p>
                  <pre className="text-emerald-300 whitespace-pre-wrap">{aggsDslExample}</pre>
                </div>
              )}

              {activeDslTab === 'java' && (
                <div className="space-y-3">
                  <p className="text-slate-400 text-xs font-sans">
                    Spring Boot 3.4 + Spring Data Elasticsearch implementation using <code>NativeQueryBuilder</code> and <code>@KafkaListener</code> CDC Ingestion.
                  </p>
                  <pre className="text-blue-300 whitespace-pre-wrap">{springDataJavaCode}</pre>
                </div>
              )}
            </div>

            {/* DSL Bottom Info */}
            <div className="bg-slate-900 border-t border-slate-800 px-3.5 py-2 flex items-center justify-between text-[11px] text-slate-400 font-mono">
              <span>POST /logistics_parcels/_search</span>
              <span className="text-emerald-400 font-semibold">200 OK • HTTP/1.1</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

// DSL Examples for Inspector
const autocompleteDslExample = `{
  "query": {
    "bool": {
      "should": [
        {
          "match": {
            "trackingNumber.suggest": {
              "query": "ORD-98",
              "boost": 5.0
            }
          }
        },
        {
          "match": {
            "recipientName.autocomplete": {
              "query": "Linh",
              "boost": 3.0
            }
          }
        },
        {
          "prefix": {
            "recipientPhone": "0984"
          }
        }
      ]
    }
  },
  "_source": ["trackingNumber", "recipientName", "recipientPhone", "destinationCity", "currentStatus"],
  "size": 5
}`;

const geoDslExample = `{
  "query": {
    "bool": {
      "filter": {
        "geo_distance": {
          "distance": "20km",
          "currentLocation": {
            "lat": 10.8015,
            "lon": 106.6644
          }
        }
      }
    }
  },
  "sort": [
    {
      "_geo_distance": {
        "currentLocation": {
          "lat": 10.8015,
          "lon": 106.6644
        },
        "order": "asc",
        "unit": "km"
      }
    }
  ]
}`;

const aggsDslExample = `{
  "size": 0,
  "aggs": {
    "status_breakdown": {
      "terms": {
        "field": "currentStatus"
      }
    },
    "destination_cities": {
      "terms": {
        "field": "destinationCity",
        "size": 10
      }
    },
    "shipping_fee_statistics": {
      "stats": {
        "field": "shippingFee"
      }
    },
    "parcels_over_time": {
      "date_histogram": {
        "field": "createdAt",
        "calendar_interval": "day"
      }
    }
  }
}`;

const springDataJavaCode = `// Spring Data Elasticsearch 8.x Query Builder
NativeQuery query = NativeQuery.builder()
    .withQuery(q -> q.bool(b -> b
        .must(m -> m.multiMatch(mm -> mm
            .query("Laptop Dell")
            .fields("trackingNumber^8", "recipientName^4", "itemDescription^3")
            .fuzziness("AUTO")
        ))
        .filter(f -> f.term(t -> t.field("currentStatus").value("IN_TRANSIT")))
        .filter(f -> f.geoDistance(gd -> gd
            .field("currentLocation")
            .distance("20km")
            .location(l -> l.latlon(ll -> ll.lat(10.8015).lon(106.6644)))
        ))
    ))
    .withHighlightQuery(new HighlightQuery(highlight, ParcelIndexDocument.class))
    .withPageable(PageRequest.of(0, 20))
    .build();

SearchHits<ParcelIndexDocument> hits = elasticsearchOperations.search(query, ParcelIndexDocument.class);

// Real-time Event-Driven Kafka CDC Consumer
@KafkaListener(topics = "logistics.tracking.event-recorded", groupId = "tracking-es-sync")
public void onTrackingEvent(String trackingNumber, String statusDesc) {
    ParcelIndexDocument doc = repository.findById(trackingNumber).orElseThrow();
    doc.setStatusDescription(statusDesc);
    doc.setLastUpdatedAt(Instant.now());
    repository.save(doc); // Ingests into Elasticsearch in < 5ms
}`;
