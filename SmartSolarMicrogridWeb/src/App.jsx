import React,{useEffect,useState,useRef} from "react";
import {Routes,Route,Navigate,useNavigate,useLocation} from "react-router-dom";
import {api,user} from "./api";
import {Sun,LayoutDashboard,Users,MapPin,Battery,CalendarDays,QrCode,LogOut,Menu,X,ShieldCheck,Activity,UserPlus,Eye,EyeOff,AlertTriangle,Search,Navigation,Crosshair,ExternalLink,CheckCircle2,Loader2,Map,Compass,Check,AlertCircle} from "lucide-react";

const save=(d)=>{localStorage.setItem("token",d.token);localStorage.setItem("user",JSON.stringify(d));};
const logout=()=>{localStorage.clear();location.href="/login"};
const auth=(roles)=>{const u=user();if(!localStorage.getItem("token"))return <Navigate to="/login"/>;if(roles&&!roles.includes(u?.role))return <Navigate to={u?.role==="Backoffice"?"/backoffice":"/operator"}/>;return null};

function Login(){
  const [f,setF]=useState({username:"",password:""}),[e,setE]=useState("");
  const [showPassword, setShowPassword] = useState(false);
  const n=useNavigate();
  async function go(x){
    x.preventDefault();
    try{
      const r=await api.post("/auth/login",f);
      save(r.data);
      n(r.data.role==="Backoffice"?"/backoffice":"/operator");
    }catch(e){
      setE(e.response?.data?.error||"Login failed");
    }
  }
  return (
    <div className="login">
      <div className="loginbox">
        <div className="logo"><Sun/></div>
        <h1>Smart Solar</h1>
        <p>Microgrid Trading System</p>
        {e&&<div className="err">{e}</div>}
        <form onSubmit={go}>
          <label>Username
            <input
              value={f.username}
              onChange={e=>setF({...f,username:e.target.value})}
              placeholder="Enter username"
              required
            />
          </label>
          <label>Password
            <div className="password-wrapper">
              <input
                type={showPassword ? "text" : "password"}
                value={f.password}
                onChange={e=>setF({...f,password:e.target.value})}
                placeholder="Enter password"
                required
              />
              <button
                type="button"
                className="password-toggle-btn"
                onClick={()=>setShowPassword(!showPassword)}
                aria-label={showPassword ? "Hide password" : "Show password"}
                title={showPassword ? "Hide password" : "Show password"}
              >
                {showPassword ? <EyeOff size={18}/> : <Eye size={18}/>}
              </button>
            </div>
          </label>
          <button type="submit">Sign in</button>
        </form>
      </div>
    </div>
  );
}

const links={Backoffice:[["/backoffice","Dashboard",LayoutDashboard],["/backoffice/users","User Management",UserPlus],["/backoffice/prosumers","Prosumer Management",Users],["/backoffice/stations","Microgrid Nodes",MapPin],["/backoffice/slots","Energy Slots",Battery],["/backoffice/reservations","Reservations",CalendarDays]],GridOperator:[["/operator","Dashboard",LayoutDashboard],["/operator/bookings","Bookings",CalendarDays],["/operator/availability","Availability",Battery],["/operator/qr","QR Verification",QrCode]]};

function Shell({role}){
  const n=useNavigate(),l=useLocation();
  const [mobileOpen, setMobileOpen] = useState(false);
  const [collapsed, setCollapsed] = useState(false);

  useEffect(()=>{ setMobileOpen(false); }, [l.pathname]);

  const toggleSidebar = () => {
    if (window.innerWidth <= 1024) {
      setMobileOpen(prev => !prev);
    } else {
      setCollapsed(prev => !prev);
    }
  };

  return (
    <div className="app-shell">
      <aside className={`side ${mobileOpen ? "open" : ""} ${collapsed ? "collapsed" : ""}`}>
        <div className="brand">
          <div className="brand-text">
            <Sun size={26}/>
            <span className="brand-title">Smart Solar</span>
          </div>
          <button className="side-close" onClick={()=>setMobileOpen(false)} aria-label="Close menu">
            <X size={20}/>
          </button>
        </div>
        {links[role].map(([p,t,I])=>(
          <button
            className={l.pathname===p?"nav active":"nav"}
            onClick={()=>{n(p);setMobileOpen(false)}}
            key={p}
            title={t}
          >
            <I size={20} className="nav-icon"/>
            <span className="nav-text">{t}</span>
          </button>
        ))}
        <button className="nav bottom" onClick={logout} title="Logout">
          <LogOut size={20} className="nav-icon"/>
          <span className="nav-text">Logout</span>
        </button>
      </aside>
      {mobileOpen && <div className="overlay" onClick={()=>setMobileOpen(false)}/>}
      <main className={`main ${collapsed ? "collapsed" : ""}`}>
        <header>
          <div className="header-left">
            <button
              className="hamb"
              onClick={toggleSidebar}
              aria-label={collapsed ? "Expand sidebar" : "Minimize sidebar"}
              title={collapsed ? "Expand sidebar" : "Minimize sidebar"}
            >
              <Menu size={20}/>
            </button>
            <span className="header-title">Smart Solar Microgrid</span>
          </div>
          <b className="user-badge">{user()?.username}</b>
        </header>
        <section>
          <Routes>
            {role==="Backoffice"?<><Route index element={<BOHome/>}/><Route path="users" element={<UserMgmt/>}/><Route path="prosumers" element={<Pros/>}/><Route path="stations" element={<Stations/>}/><Route path="slots" element={<Slots/>}/><Route path="reservations" element={<Res/>}/></>:<><Route index element={<OpHome/>}/><Route path="bookings" element={<Bookings/>}/><Route path="availability" element={<Avail/>}/><Route path="qr" element={<QR/>}/></>}
          </Routes>
        </section>
      </main>
    </div>
  );
}

function Guard({role}){return auth([role])||<Shell role={role}/>}

const Stat=({t,v,I})=><div className="card stat"><div><small>{t}</small><strong>{v??"—"}</strong></div><I/></div>;
const Title=({t,d})=><div className="title"><h1>{t}</h1><p>{d}</p></div>;
const Table=({children})=><div className="card table">{children}</div>;

function BOHome(){const[d,setD]=useState({});useEffect(()=>{api.get("/dashboard/backoffice").then(x=>setD(x.data))},[]);return <><Title t="Backoffice Dashboard" d="Administration and microgrid overview"/><div className="stats"><Stat t="Active Prosumers" v={d.totalProsumers} I={Users}/><Stat t="Pending Accounts" v={d.pendingProsumerAccounts} I={ShieldCheck}/><Stat t="Microgrid Nodes" v={d.totalStations} I={MapPin}/><Stat t="Available Slots" v={d.availableSlots} I={Battery}/><Stat t="Pending Reservations" v={d.pendingReservations} I={CalendarDays}/><Stat t="Approved Reservations" v={d.approvedReservations} I={Activity}/><Stat t="Completed Transfers" v={d.completedTransfers} I={QrCode}/></div></>}

function UserMgmt() {
    const empty = { username: "", password: "", confirmPassword: "", role: 1, status: 0 };
    const [f, setF] = useState(empty), [a, setA] = useState([]), [e, setE] = useState(""), [filter, setFilter] = useState("All Roles"), [edit, setEdit] = useState(null);
    const load = () => api.get("/users/internal").then(x => setA(x.data));
    useEffect(() => { load(); }, []);
    async function saveF(x) {
        x.preventDefault();
        if (f.password !== f.confirmPassword) return setE("Passwords do not match");
        try {
            if (edit) {
                await api.put(`/users/${edit}`, { role: +f.role, status: +f.status, ...(f.password ? { password: f.password } : {}) });
            } else {
                await api.post("/users", { username: f.username, password: f.password, role: +f.role, status: +f.status });
            }
            setF(empty); setEdit(null); setE(""); load();
        } catch (err) {
            setE(err.response?.data?.error || "Failed to save user");
        }
    }
    async function del(id) {
        if (window.confirm("Are you sure you want to delete this user?")) {
            try { await api.delete(`/users/${id}`); load(); }
            catch (err) { setE(err.response?.data?.error || "Failed to delete"); }
        }
    }
    function startEdit(u) {
        setEdit(u.id);
        setF({ username: u.username, password: "", confirmPassword: "", role: u.role, status: u.status });
        setE("");
    }
    const fa = filter === "All Roles" ? a : a.filter(u => u.role === (filter === "Backoffice" ? 0 : 1));
    return <><Title t="User Management" d="Manage internal Backoffice and Grid Operator accounts" /><div className="grid2"><form className="card" onSubmit={saveF}><h2>{edit ? "Edit User" : "Create User"}</h2>{e && <div className="err">{e}</div>}<label>Username<input required={!edit} disabled={!!edit} value={f.username} onChange={e => setF({ ...f, username: e.target.value })} /></label><label>Password<input required={!edit} type="password" value={f.password} placeholder={edit ? "Leave blank to keep current" : ""} onChange={e => setF({ ...f, password: e.target.value })} /></label><label>Confirm Password<input required={!edit && f.password} type="password" value={f.confirmPassword} onChange={e => setF({ ...f, confirmPassword: e.target.value })} /></label><label>Role<select value={f.role} onChange={e => setF({ ...f, role: e.target.value })}><option value={0}>Backoffice</option><option value={1}>Grid Operator</option></select></label><label>Status<select value={f.status} onChange={e => setF({ ...f, status: e.target.value })}><option value={0}>Active</option><option value={1}>Inactive</option></select></label><div className="form-actions"><button type="submit" style={{flex:1}}>{edit ? "Update User" : "Create User"}</button>{edit && <button type="button" onClick={() => { setEdit(null); setF(empty); setE(""); }} style={{background:"#94a3b8",flex:1}}>Cancel</button>}</div></form><Table><div className="table-header"><h2 style={{ margin: 0 }}>Internal Users</h2><select value={filter} onChange={e => setFilter(e.target.value)} style={{ padding: "6px 12px", borderRadius: "8px", border: "1px solid #cbd5e1", width: "auto", marginTop: 0, fontWeight: 600, color: "#475569", cursor: "pointer" }}><option>All Roles</option><option>Backoffice</option><option>Grid Operator</option></select></div><table><thead><tr><th>Username</th><th>Role</th><th>Status</th><th>Created</th><th>Actions</th></tr></thead><tbody>{fa.length > 0 ? fa.map(u => <tr key={u.id}><td>{u.username}</td><td>{u.role === 0 ? "Backoffice" : "Grid Operator"}</td><td>{u.status === 0 ? "Active" : "Inactive"}</td><td>{new Date(u.createdAt).toLocaleDateString()}</td><td><button className="btn-edit" onClick={() => startEdit(u)}>Edit</button><button className="btn-delete" onClick={() => del(u.id)}>Delete</button></td></tr>) : <tr><td colSpan="5" style={{ textAlign: "center", padding: "20px", color: "#64748b" }}>No users found for this role.</td></tr>}</tbody></table></Table></div></>;
}

function Pros(){
    const [a, setA] = useState([]), [st, setSt] = useState("Pending");
    const load = () => api.get("/prosumers/all").then(x => setA(x.data)).catch(() => {});
    useEffect(() => { load(); }, []);
    const act = async (n, action) => { await api.put(`/prosumers/${n}/${action}`); load(); };
    const filtered = a.filter(p => st === "Pending" ? (p.accountStatus === 0 || p.accountStatus === "Pending") : st === "Active" ? (p.accountStatus === 1 || p.accountStatus === "Active") : (p.accountStatus === 2 || p.accountStatus === "Deactivated"));
    return <><Title t="Prosumer Management" d="Manage prosumer profiles and lifecycle" /><Table>
        <div className="tabs">
            <button className={st === "Pending" ? "selected" : ""} onClick={() => setSt("Pending")}>Pending</button>
            <button className={st === "Active" ? "selected" : ""} onClick={() => setSt("Active")}>Active</button>
            <button className={st === "Inactive" ? "selected" : ""} onClick={() => setSt("Inactive")}>Deactivated</button>
        </div>
        <table>
            <thead><tr><th>NIC</th><th>Name</th><th>Email</th><th>Phone</th><th>Status</th><th>Actions</th></tr></thead>
            <tbody>{filtered.map(p => <tr key={p.id}>
                <td>{p.nic}</td><td>{p.fullName}</td><td>{p.email}</td><td>{p.phone}</td>
                <td><span className="badge">{p.accountStatus === 0 || p.accountStatus === "Pending" ? "Pending" : p.accountStatus === 1 || p.accountStatus === "Active" ? "Active" : "Inactive"}</span></td>
                <td>
                    {st === "Pending" && <button className="btn-success" onClick={() => act(p.nic, "activate")}>Activate</button>}
                    {st === "Active" && <button className="btn-delete" onClick={() => act(p.nic, "deactivate")}>Deactivate</button>}
                    {st === "Inactive" && <button className="btn-success" onClick={() => act(p.nic, "reactivate")}>Reactivate</button>}
                </td>
            </tr>)}</tbody>
        </table>
        {!filtered.length && <p className="empty">No {st.toLowerCase()} prosumers found.</p>}
    </Table></>;
}
function LocationPickerModal({ initialLat, initialLng, initialName, isOpen, onClose, onSelectLocation }) {
    const [searchQuery, setSearchQuery] = useState("");
    const [searchResults, setSearchResults] = useState([]);
    const [isSearching, setIsSearching] = useState(false);
    const [searchError, setSearchError] = useState("");
    const [isMapReady, setIsMapReady] = useState(false);
    const [selectedCoords, setSelectedCoords] = useState([
        initialLat && !isNaN(+initialLat) ? +initialLat : 6.9271,
        initialLng && !isNaN(+initialLng) ? +initialLng : 79.8612
    ]);
    const [selectedPlaceName, setSelectedPlaceName] = useState(initialName || "");
    const [isLocating, setIsLocating] = useState(false);

    const mapContainerRef = useRef(null);
    const mapInstanceRef = useRef(null);
    const markerRef = useRef(null);

    // Instant local Sri Lanka & Renewable Energy Hubs dataset for zero-latency search
    const popularLocations = [
        { name: "Hambantota Solar Park (100MW)", lat: 6.1429, lng: 81.1212, category: "Solar Park" },
        { name: "Mirijjawila Solar Power Station", lat: 6.1685, lng: 81.0820, category: "Solar Park" },
        { name: "Siyambalanduwa Renewable Hub", lat: 6.9064, lng: 81.5542, category: "Solar Station" },
        { name: "Pooneryn Renewable Energy Park", lat: 9.5028, lng: 80.2014, category: "Solar & Wind Hub" },
        { name: "Maduru Oya Floating Solar Facility", lat: 7.6481, lng: 81.2185, category: "Floating Solar" },
        { name: "Colombo Central Solar Hub", lat: 6.9271, lng: 79.8612, category: "Urban Hub" },
        { name: "Colombo Fort Grid Substation", lat: 6.9344, lng: 79.8428, category: "Grid Station" },
        { name: "Kandy Central Solar Station", lat: 7.2906, lng: 80.6337, category: "Regional Hub" },
        { name: "Galle Southern Microgrid", lat: 6.0535, lng: 80.2210, category: "Regional Hub" },
        { name: "Anuradhapura Solar Node", lat: 8.3114, lng: 80.4037, category: "North Central Hub" },
        { name: "Jaffna Peninsula Solar Hub", lat: 9.6615, lng: 80.0255, category: "Northern Hub" },
        { name: "Trincomalee Deep Bay Station", lat: 8.5874, lng: 81.2152, category: "Eastern Hub" },
        { name: "Kurunegala Wayamba Solar Hub", lat: 7.4863, lng: 80.3623, category: "Regional Hub" },
        { name: "Negombo Coastal Solar Node", lat: 7.2008, lng: 79.8736, category: "Coastal Node" },
        { name: "Matara Southern Grid Station", lat: 5.9549, lng: 80.5550, category: "Regional Hub" },
        { name: "Batticaloa East Coast Solar", lat: 7.7310, lng: 81.6747, category: "Regional Hub" },
        { name: "Polonnaruwa Ancient City Solar", lat: 7.9403, lng: 81.0188, category: "North Central Node" },
        { name: "Badulla Uva Solar Station", lat: 6.9934, lng: 81.0550, category: "Uva Hub" },
        { name: "Ratnapura Sabaragamuwa Grid", lat: 6.6828, lng: 80.4010, category: "Regional Hub" },
        { name: "Nuwara Eliya Hill Station", lat: 6.9497, lng: 80.7891, category: "Central Grid" },
        { name: "Puttalam Wind & Solar Complex", lat: 8.0362, lng: 79.8283, category: "Renewable Hub" },
        { name: "Mannar Island Energy Node", lat: 8.9810, lng: 79.9042, category: "Renewable Hub" }
    ];

    const presetRegions = [
        { name: "Colombo", lat: 6.9271, lng: 79.8612 },
        { name: "Hambantota", lat: 6.1429, lng: 81.1212 },
        { name: "Kandy", lat: 7.2906, lng: 80.6337 },
        { name: "Galle", lat: 6.0535, lng: 80.2210 },
        { name: "Anuradhapura", lat: 8.3114, lng: 80.4037 },
        { name: "Jaffna", lat: 9.6615, lng: 80.0255 },
        { name: "Trincomalee", lat: 8.5874, lng: 81.2152 }
    ];

    // Multi-CDN Leaflet dynamic loader
    const ensureLeaflet = () => {
        return new Promise((resolve) => {
            if (window.L && typeof window.L.map === "function") {
                resolve(window.L);
                return;
            }

            if (!document.getElementById("leaflet-css")) {
                const link = document.createElement("link");
                link.id = "leaflet-css";
                link.rel = "stylesheet";
                link.href = "https://unpkg.com/leaflet@1.9.4/dist/leaflet.css";
                document.head.appendChild(link);
            }

            if (!document.getElementById("leaflet-js")) {
                const script = document.createElement("script");
                script.id = "leaflet-js";
                script.src = "https://unpkg.com/leaflet@1.9.4/dist/leaflet.js";
                script.onload = () => resolve(window.L);
                script.onerror = () => {
                    const fallback = document.createElement("script");
                    fallback.src = "https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/leaflet.js";
                    fallback.onload = () => resolve(window.L);
                    fallback.onerror = () => resolve(null);
                    document.head.appendChild(fallback);
                };
                document.head.appendChild(script);
            } else {
                let attempts = 0;
                const check = setInterval(() => {
                    attempts++;
                    if (window.L && typeof window.L.map === "function") {
                        clearInterval(check);
                        resolve(window.L);
                    } else if (attempts > 30) {
                        clearInterval(check);
                        resolve(null);
                    }
                }, 100);
            }
        });
    };

    useEffect(() => {
        if (!isOpen) return;

        const lat = initialLat && !isNaN(+initialLat) ? +initialLat : 6.9271;
        const lng = initialLng && !isNaN(+initialLng) ? +initialLng : 79.8612;
        setSelectedCoords([lat, lng]);
        setSelectedPlaceName(initialName || "");
        setSearchQuery("");
        setSearchResults([]);
        setSearchError("");

        let active = true;

        ensureLeaflet().then((L) => {
            if (!active || !L || !mapContainerRef.current) return;

            try {
                if (mapContainerRef.current._leaflet_id) {
                    delete mapContainerRef.current._leaflet_id;
                }

                if (mapInstanceRef.current) {
                    mapInstanceRef.current.remove();
                    mapInstanceRef.current = null;
                }

                const createPin = () => {
                    return L.divIcon({
                        className: 'custom-map-marker-pin',
                        html: `<div style="background:#10b981;width:26px;height:26px;border-radius:50%;border:3px solid #ffffff;box-shadow:0 0 20px rgba(16,185,129,0.9);display:flex;align-items:center;justify-content:center;cursor:pointer;"><div style="background:#ffffff;width:8px;height:8px;border-radius:50%;"></div></div>`,
                        iconSize: [26, 26],
                        iconAnchor: [13, 13]
                    });
                };

                const map = L.map(mapContainerRef.current, {
                    center: [lat, lng],
                    zoom: 12,
                    zoomControl: true,
                    attributionControl: true
                });

                L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
                    maxZoom: 19,
                    attribution: '&copy; OpenStreetMap'
                }).addTo(map);

                const marker = L.marker([lat, lng], {
                    draggable: true,
                    icon: createPin()
                }).addTo(map);

                marker.on("dragend", (e) => {
                    const pos = e.target.getLatLng();
                    const newLat = Number(pos.lat.toFixed(6));
                    const newLng = Number(pos.lng.toFixed(6));
                    setSelectedCoords([newLat, newLng]);
                });

                map.on("click", (e) => {
                    const newLat = Number(e.latlng.lat.toFixed(6));
                    const newLng = Number(e.latlng.lng.toFixed(6));
                    setSelectedCoords([newLat, newLng]);
                    marker.setLatLng([newLat, newLng]);
                });

                mapInstanceRef.current = map;
                markerRef.current = marker;
                setIsMapReady(true);

                // Multiple invalidation passes to prevent grey map tiles
                setTimeout(() => map && map.invalidateSize(), 150);
                setTimeout(() => map && map.invalidateSize(), 350);
            } catch (err) {
                console.error("Map initialization error:", err);
            }
        });

        return () => {
            active = false;
        };
    }, [isOpen, initialLat, initialLng, initialName]);

    useEffect(() => {
        if (!isOpen) {
            setIsMapReady(false);
            if (mapInstanceRef.current) {
                mapInstanceRef.current.remove();
                mapInstanceRef.current = null;
                markerRef.current = null;
            }
        }
    }, [isOpen]);

    const updateMapPosition = (lat, lng, zoom = 14, name = "") => {
        const fixedLat = Number(Number(lat).toFixed(6));
        const fixedLng = Number(Number(lng).toFixed(6));
        setSelectedCoords([fixedLat, fixedLng]);
        if (name) setSelectedPlaceName(name);

        if (mapInstanceRef.current) {
            mapInstanceRef.current.setView([fixedLat, fixedLng], zoom);
            if (markerRef.current) {
                markerRef.current.setLatLng([fixedLat, fixedLng]);
            }
            setTimeout(() => mapInstanceRef.current?.invalidateSize(), 100);
        }
    };

    // Live search combining instant local database + Nominatim API
    const handleSearch = async (e) => {
        if (e) e.preventDefault();
        const query = searchQuery.trim().toLowerCase();
        if (!query) return;

        setIsSearching(true);
        setSearchError("");

        // 1. Instant local matching
        const localMatches = popularLocations.filter(loc => 
            loc.name.toLowerCase().includes(query) || 
            loc.category.toLowerCase().includes(query)
        ).map(loc => ({
            display_name: `${loc.name} (${loc.category})`,
            lat: loc.lat.toString(),
            lon: loc.lng.toString(),
            isLocal: true
        }));

        setSearchResults(localMatches);

        // 2. Fetch live global/regional geocoding
        try {
            const controller = new AbortController();
            const timeoutId = setTimeout(() => controller.abort(), 6000);

            const res = await fetch(
                `https://nominatim.openstreetmap.org/search?format=json&q=${encodeURIComponent(query)}&limit=6`,
                { signal: controller.signal }
            );
            clearTimeout(timeoutId);

            if (res.ok) {
                const apiData = await res.json();
                if (Array.isArray(apiData) && apiData.length > 0) {
                    const combined = [...localMatches];
                    apiData.forEach(item => {
                        if (!combined.some(c => Math.abs(parseFloat(c.lat) - parseFloat(item.lat)) < 0.01 && Math.abs(parseFloat(c.lon) - parseFloat(item.lon)) < 0.01)) {
                            combined.push(item);
                        }
                    });
                    setSearchResults(combined);
                } else if (localMatches.length === 0) {
                    setSearchError("No locations found for this query. Try a city or district name (e.g. Kandy, Hambantota, Galle).");
                }
            } else if (localMatches.length === 0) {
                setSearchError("No results found. Please check spelling or select from quick hubs below.");
            }
        } catch (err) {
            if (localMatches.length === 0) {
                setSearchError("Could not reach online geocoding. Please pick a location from the map or quick hubs.");
            }
        } finally {
            setIsSearching(false);
        }
    };

    // Live auto-filter as user types
    const handleInputChange = (e) => {
        const val = e.target.value;
        setSearchQuery(val);
        if (!val.trim()) {
            setSearchResults([]);
            setSearchError("");
            return;
        }

        const query = val.toLowerCase().trim();
        const instantMatches = popularLocations.filter(loc => 
            loc.name.toLowerCase().includes(query) || 
            loc.category.toLowerCase().includes(query)
        ).map(loc => ({
            display_name: `${loc.name} (${loc.category})`,
            lat: loc.lat.toString(),
            lon: loc.lng.toString(),
            isLocal: true
        }));

        if (instantMatches.length > 0) {
            setSearchResults(instantMatches);
            setSearchError("");
        }
    };

    const selectSearchResult = (item) => {
        const lat = parseFloat(item.lat);
        const lon = parseFloat(item.lon);
        const shortName = item.display_name.split(",")[0].split("(")[0].trim();
        updateMapPosition(lat, lon, 14, shortName);
        setSearchResults([]);
        setSearchQuery(item.display_name.split(",")[0]);
    };

    const detectCurrentLocation = () => {
        if (!navigator.geolocation) {
            setSearchError("Geolocation is not supported by your browser.");
            return;
        }
        setIsLocating(true);
        setSearchError("");
        navigator.geolocation.getCurrentPosition(
            (pos) => {
                const lat = pos.coords.latitude;
                const lng = pos.coords.longitude;
                updateMapPosition(lat, lng, 15, "Current GPS Device Location");
                setIsLocating(false);
            },
            (err) => {
                setIsLocating(false);
                let msg = "Could not retrieve GPS location.";
                if (err.code === 1) msg = "Location permission denied in browser.";
                else if (err.code === 2) msg = "Position unavailable from GPS.";
                else if (err.code === 3) msg = "GPS location request timed out.";
                setSearchError(msg);
            },
            { enableHighAccuracy: true, timeout: 10000, maximumAge: 0 }
        );
    };

    const handleConfirm = () => {
        onSelectLocation({
            latitude: selectedCoords[0],
            longitude: selectedCoords[1],
            placeName: selectedPlaceName
        });
        onClose();
    };

    if (!isOpen) return null;

    return (
        <div className="modal-overlay" onClick={onClose}>
            <div className="map-picker-modal" onClick={e => e.stopPropagation()}>
                <div className="map-picker-header">
                    <div className="map-picker-title">
                        <Compass size={24} style={{ color: "var(--accent-primary)" }} />
                        <div>
                            <h3>Select Node Location</h3>
                            <p>Search any city/address or click anywhere on the map to position the microgrid pin</p>
                        </div>
                    </div>
                    <button className="modal-close-btn" onClick={onClose} aria-label="Close">
                        <X size={20} />
                    </button>
                </div>

                <div className="map-picker-body">
                    {/* Search Bar & Auto-Detect Controls */}
                    <div className="map-picker-controls">
                        <form onSubmit={handleSearch} className="map-search-form">
                            <div className="map-search-input-wrap">
                                <Search size={16} />
                                <input
                                    type="text"
                                    placeholder="Search city, town, or solar park (e.g. Hambantota, Kandy, Galle)..."
                                    value={searchQuery}
                                    onChange={handleInputChange}
                                />
                                {searchQuery && (
                                    <button
                                        type="button"
                                        className="search-clear-btn"
                                        onClick={() => { setSearchQuery(""); setSearchResults([]); }}
                                    >
                                        <X size={14} />
                                    </button>
                                )}
                            </div>
                            <button type="submit" className="map-search-btn" disabled={isSearching}>
                                {isSearching ? <Loader2 size={15} className="spin-icon" /> : "Search"}
                            </button>
                        </form>

                        <button
                            type="button"
                            className="map-my-location-btn"
                            onClick={detectCurrentLocation}
                            disabled={isLocating}
                            title="Center on my current GPS location"
                        >
                            {isLocating ? <Loader2 size={14} className="spin-icon" /> : <Navigation size={14} />}
                            <span>My GPS</span>
                        </button>
                    </div>

                    {/* Search Results Dropdown */}
                    {searchResults.length > 0 && (
                        <div className="map-search-dropdown">
                            <div className="map-search-dropdown-title">Matching Locations (Click to place marker):</div>
                            {searchResults.map((item, idx) => (
                                <div
                                    key={idx}
                                    className="map-search-item"
                                    onClick={() => selectSearchResult(item)}
                                >
                                    <MapPin size={14} style={{ color: "var(--accent-primary)", flexShrink: 0 }} />
                                    <span className="map-search-item-text">{item.display_name}</span>
                                </div>
                            ))}
                        </div>
                    )}

                    {searchError && (
                        <div className="map-error-alert">
                            <AlertCircle size={15} style={{ flexShrink: 0 }} />
                            <span>{searchError}</span>
                        </div>
                    )}

                    {/* Quick Preset Regions */}
                    <div className="preset-chips-container">
                        <span className="preset-label">Quick Hubs:</span>
                        <div className="preset-chips-list">
                            {presetRegions.map(p => (
                                <button
                                    key={p.name}
                                    type="button"
                                    className="preset-region-chip"
                                    onClick={() => updateMapPosition(p.lat, p.lng, 13, p.name)}
                                >
                                    <MapPin size={11} />
                                    {p.name}
                                </button>
                            ))}
                        </div>
                    </div>

                    {/* Interactive Map Canvas */}
                    <div className="map-canvas-container">
                        <div ref={mapContainerRef} className="leaflet-map-canvas" />
                        <div className="map-hint-badge">
                            <Crosshair size={13} /> Click map or drag marker to set exact coordinates
                        </div>
                    </div>
                </div>

                {/* Bottom Bar */}
                <div className="map-picker-footer">
                    <div className="map-coords-display">
                        <div className="map-coords-row">
                            <span className="coord-label">Latitude:</span>
                            <strong className="coord-value">{selectedCoords[0]}</strong>
                        </div>
                        <div className="map-coords-row">
                            <span className="coord-label">Longitude:</span>
                            <strong className="coord-value">{selectedCoords[1]}</strong>
                        </div>
                        {selectedPlaceName && (
                            <div className="map-coords-row location-tag">
                                <MapPin size={12} style={{ color: "var(--accent-primary)" }} />
                                <span>{selectedPlaceName}</span>
                            </div>
                        )}
                    </div>

                    <div className="map-footer-actions">
                        <button type="button" className="btn-cancel" onClick={onClose}>
                            Cancel
                        </button>
                        <button type="button" className="btn-apply-location" onClick={handleConfirm}>
                            <Check size={16} /> Apply Location
                        </button>
                    </div>
                </div>
            </div>
        </div>
    );
}

function Stations() {
    const [a, setA] = useState([]), [edit, setEdit] = useState(null), [err, setErr] = useState("");
    const [search, setSearch] = useState("");
    const [deactModal, setDeactModal] = useState(null);
    const [deactReason, setDeactReason] = useState("");
    const [modalErr, setModalErr] = useState("");
    const [gpsLoading, setGpsLoading] = useState(false);
    const [gpsStatus, setGpsStatus] = useState(null);
    const [isMapModalOpen, setIsMapModalOpen] = useState(false);

    const getNextCode = (nodes) => {
        if (!nodes || nodes.length === 0) return "N-001";
        const max = Math.max(...nodes.map(n => {
            const match = n.nodeCode.match(/N-(\d+)/);
            return match ? parseInt(match[1]) : 0;
        }));
        return `N-${(max + 1).toString().padStart(3, '0')}`;
    };

    const empty = (nodes = []) => ({ nodeCode: getNextCode(nodes), nodeName: "", latitude: "", longitude: "", capacityKw: "", batterySlotAvailability: "", scheduleStart: "08:00", scheduleEnd: "18:00" });
    const [f, setF] = useState(empty());

    const load = () => api.get("/microgrid-nodes").then(x => {
        setA(x.data);
        if (!edit) setF(empty(x.data));
    });
    useEffect(() => { load(); }, []);

    const getCurrentGpsLocation = () => {
        if (!navigator.geolocation) {
            setGpsStatus({ type: "error", message: "Geolocation is not supported by your browser." });
            return;
        }
        setGpsLoading(true);
        setGpsStatus(null);
        navigator.geolocation.getCurrentPosition(
            (position) => {
                const lat = Number(position.coords.latitude.toFixed(6));
                const lng = Number(position.coords.longitude.toFixed(6));
                const acc = Math.round(position.coords.accuracy);
                setF(prev => ({ ...prev, latitude: lat, longitude: lng }));
                setGpsStatus({
                    type: "success",
                    message: `GPS acquired: ${lat}, ${lng} (Accuracy: ±${acc}m)`
                });
                setGpsLoading(false);
            },
            (error) => {
                let msg = "Failed to retrieve GPS location.";
                if (error.code === error.PERMISSION_DENIED) {
                    msg = "Location permission denied. Please enable location access in browser settings.";
                } else if (error.code === error.POSITION_UNAVAILABLE) {
                    msg = "Location information is unavailable.";
                } else if (error.code === error.TIMEOUT) {
                    msg = "GPS location request timed out. Please retry.";
                }
                setGpsStatus({ type: "error", message: msg });
                setGpsLoading(false);
            },
            {
                enableHighAccuracy: true,
                timeout: 10000,
                maximumAge: 0
            }
        );
    };

    const handleLocationSelected = ({ latitude, longitude, placeName }) => {
        setF(prev => ({
            ...prev,
            latitude,
            longitude,
            nodeName: prev.nodeName ? prev.nodeName : (placeName ? `${placeName} Solar Hub` : prev.nodeName)
        }));
        setGpsStatus({
            type: "success",
            message: `Coordinates updated: ${latitude}, ${longitude}${placeName ? ` (${placeName})` : ''}`
        });
    };

    async function saveF(e) {
        e.preventDefault();
        const b = { 
            ...f, 
            latitude: +f.latitude, 
            longitude: +f.longitude, 
            capacityKw: +f.capacityKw, 
            batterySlotAvailability: +f.batterySlotAvailability,
            scheduleStart: f.scheduleStart || "08:00",
            scheduleEnd: f.scheduleEnd || "18:00"
        };
        try {
            if (edit) await api.put(`/microgrid-nodes/${edit}`, b);
            else await api.post("/microgrid-nodes", b);
            setEdit(null);
            setGpsStatus(null);
            setErr("");
            load();
        } catch (x) {
            setErr(x.response?.data?.error || "Failed to save station");
        }
    }

    const suggestions = [
        "Routine Maintenance",
        "Grid Upgrading / Maintenance",
        "Technical Fault / Breakdown",
        "Emergency Safety Shutdown",
        "Adverse Weather / Inspection"
    ];

    function openDeactivateModal(node) {
        setDeactModal(node);
        setDeactReason("Routine Maintenance");
        setModalErr("");
    }

    async function confirmDeactivate(e) {
        e.preventDefault();
        if (!deactReason.trim()) {
            setModalErr("Admin note / reason is required for deactivation.");
            return;
        }
        try {
            await api.put(`/microgrid-nodes/${deactModal.id}/deactivate`, { reason: deactReason.trim() });
            setDeactModal(null);
            setDeactReason("");
            setModalErr("");
            setErr("");
            load();
        } catch (x) {
            setModalErr(x.response?.data?.error || "Cannot deactivate node");
        }
    }

    async function activate(id) {
        try {
            await api.put(`/microgrid-nodes/${id}/activate`);
            setErr("");
            load();
        } catch (x) {
            setErr(x.response?.data?.error || "Cannot activate node");
        }
    }

    const filteredNodes = a.filter(x => {
        if (!search.trim()) return true;
        const q = search.toLowerCase();
        return (
            x.nodeCode?.toLowerCase().includes(q) ||
            x.nodeName?.toLowerCase().includes(q) ||
            (x.status === 0 || x.status === "Active" ? "active" : "inactive").includes(q) ||
            x.adminNote?.toLowerCase().includes(q) ||
            x.deactivationReason?.toLowerCase().includes(q) ||
            `${x.latitude},${x.longitude}`.includes(q)
        );
    });

    const inactiveMatches = search.trim() ? filteredNodes.filter(x => x.status !== 0) : [];

    const hasCoords = f.latitude !== "" && f.longitude !== "" && !isNaN(+f.latitude) && !isNaN(+f.longitude);

    return <>
        <Title t="Microgrid Nodes" d="Register and manage solar grid hubs" />
        <div className="grid2">
            <form className="card" onSubmit={saveF}>
                <h2>{edit ? "Update" : "Register"} Station</h2>
                {err && <div className="err">{err}</div>}
                
                <label>
                    Node Code
                    <input
                        required
                        disabled={!!edit}
                        value={f.nodeCode || ''}
                        type="text"
                        placeholder="e.g. N-003"
                        onChange={e => setF({ ...f, nodeCode: e.target.value })}
                    />
                </label>

                <label>
                    Branch / Station Name
                    <input
                        required
                        value={f.nodeName || ''}
                        type="text"
                        placeholder="e.g. Colombo Central Solar Hub"
                        onChange={e => setF({ ...f, nodeName: e.target.value })}
                    />
                </label>

                {/* GPS Location Section */}
                <div className="gps-section">
                    <div className="gps-section-header">
                        <span className="gps-section-title">
                            <Crosshair size={16} /> Location Coordinates
                        </span>
                        <div className="gps-actions-row">
                            <button
                                type="button"
                                className="gps-btn map-pick-btn"
                                onClick={() => setIsMapModalOpen(true)}
                                title="Open interactive map to search any location or click to pick coordinates"
                            >
                                <Map size={14} /> Map Picker & Search
                            </button>
                            <button
                                type="button"
                                className="gps-btn"
                                disabled={gpsLoading}
                                onClick={getCurrentGpsLocation}
                                title="Auto-detect current device GPS location"
                            >
                                {gpsLoading ? (
                                    <>
                                        <Loader2 size={14} className="spin-icon" /> Locating...
                                    </>
                                ) : (
                                    <>
                                        <Navigation size={14} /> My GPS
                                    </>
                                )}
                            </button>
                        </div>
                    </div>

                    {gpsStatus && (
                        <div className={`gps-status-box ${gpsStatus.type}`}>
                            {gpsStatus.type === "success" ? (
                                <CheckCircle2 size={15} style={{ flexShrink: 0 }} />
                            ) : (
                                <AlertTriangle size={15} style={{ flexShrink: 0 }} />
                            )}
                            <span>{gpsStatus.message}</span>
                        </div>
                    )}

                    <div className="gps-coords-grid">
                        <label>
                            Latitude
                            <input
                                required
                                value={f.latitude !== undefined ? f.latitude : ''}
                                type="number"
                                min="-90"
                                max="90"
                                step="0.000001"
                                placeholder="e.g. 6.927100"
                                onChange={e => setF({ ...f, latitude: e.target.value })}
                            />
                        </label>
                        <label>
                            Longitude
                            <input
                                required
                                value={f.longitude !== undefined ? f.longitude : ''}
                                type="number"
                                min="-180"
                                max="180"
                                step="0.000001"
                                placeholder="e.g. 79.861200"
                                onChange={e => setF({ ...f, longitude: e.target.value })}
                            />
                        </label>
                    </div>

                    {hasCoords && (
                        <div className="gps-preview-bar">
                            <a
                                href={`https://www.google.com/maps?q=${f.latitude},${f.longitude}`}
                                target="_blank"
                                rel="noopener noreferrer"
                                className="gps-map-link"
                            >
                                <ExternalLink size={13} /> View on Google Maps ({f.latitude}, {f.longitude})
                            </a>
                        </div>
                    )}
                </div>

                <label>
                    Capacity (kW)
                    <input
                        required
                        value={f.capacityKw || ''}
                        type="number"
                        min="0.1"
                        step="0.1"
                        placeholder="e.g. 150"
                        onChange={e => setF({ ...f, capacityKw: e.target.value })}
                    />
                </label>

                <label>
                    Battery Slot Availability
                    <input
                        required
                        value={f.batterySlotAvailability !== undefined ? f.batterySlotAvailability : ''}
                        type="number"
                        min="0"
                        step="1"
                        placeholder="e.g. 20"
                        onChange={e => setF({ ...f, batterySlotAvailability: e.target.value })}
                    />
                </label>

                <div className="form-actions">
                    <button type="submit" style={{flex:1}}> {edit ? "Update" : "Create Node"} </button>
                    {edit && <button type="button" onClick={() => { setEdit(null); setF(empty(a)); setGpsStatus(null); setErr(""); }} style={{background:"#94a3b8",flex:1}}>Cancel</button>}
                </div>
            </form>

            <LocationPickerModal
                isOpen={isMapModalOpen}
                onClose={() => setIsMapModalOpen(false)}
                initialLat={f.latitude}
                initialLng={f.longitude}
                initialName={f.nodeName}
                onSelectLocation={handleLocationSelected}
            />

            <Table>
                <div className="table-header">
                    <h2 style={{ margin: 0 }}>Registered Nodes</h2>
                    <div className="table-search-bar">
                        <Search size={15}/>
                        <input
                            type="text"
                            placeholder="Search branch name, code, note..."
                            value={search}
                            onChange={e => setSearch(e.target.value)}
                        />
                        {search && (
                            <button type="button" onClick={() => setSearch("")} style={{ background: "transparent", border: "none", color: "#94a3b8", cursor: "pointer", padding: "0 4px", display: "flex", alignItems: "center" }}>
                                <X size={14}/>
                            </button>
                        )}
                    </div>
                </div>

                {inactiveMatches.length > 0 && (
                    <div className="inactive-search-alert">
                        <AlertTriangle size={20}/>
                        <div>
                            <strong>Deactivated Branch Notice:</strong>
                            {inactiveMatches.map(n => (
                                <div key={n.id} style={{ marginTop: '3px', fontSize: '13px' }}>
                                    Hub <strong>{n.nodeCode} ({n.nodeName})</strong> is currently <u>Unavailable</u>.
                                    <br/>
                                    <strong>Admin Reason:</strong> {n.adminNote || n.deactivationReason || "Routine Maintenance"}
                                </div>
                            ))}
                        </div>
                    </div>
                )}

                <table>
                    <thead>
                        <tr>
                            <th>Code</th>
                            <th>Name</th>
                            <th>GPS Coordinates</th>
                            <th>Capacity</th>
                            <th>Battery</th>
                            <th>Status</th>
                            <th>Admin Note</th>
                            <th style={{ textAlign: "right", paddingRight: "18px" }}>Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        {filteredNodes.length > 0 ? filteredNodes.map(x => (
                            <tr key={x.id}>
                                <td><strong>{x.nodeCode}</strong></td>
                                <td>{x.nodeName}</td>
                                <td>
                                    <a
                                        href={`https://www.google.com/maps?q=${x.latitude},${x.longitude}`}
                                        target="_blank"
                                        rel="noopener noreferrer"
                                        className="table-gps-link"
                                        title="Click to view location in Google Maps"
                                    >
                                        <MapPin size={13} className="gps-pin-icon" />
                                        <span>{x.latitude}, {x.longitude}</span>
                                        <ExternalLink size={11} className="gps-ext-icon" />
                                    </a>
                                </td>
                                <td>{x.capacityKw} kW</td>
                                <td>{x.batterySlotAvailability}</td>
                                <td>
                                    <span className={`badge ${x.status === 0 || x.status === "Active" ? "badge-active" : "badge-inactive"}`}>
                                        {x.status === 0 || x.status === "Active" ? "Active" : "Inactive"}
                                    </span>
                                </td>
                                <td>
                                    {x.status !== 0 && x.status !== "Active" ? (
                                        <span className="reason-pill" title={`Admin Reason: ${x.adminNote || x.deactivationReason || "Routine Maintenance"}`}>
                                            <AlertTriangle size={12} style={{ flexShrink: 0 }}/>
                                            <span>{x.adminNote || x.deactivationReason || "Routine Maintenance"}</span>
                                        </span>
                                    ) : (
                                        <span className="text-muted-dash">—</span>
                                    )}
                                </td>
                                <td>
                                    <div className="table-actions-cell" style={{ justifyContent: "flex-end" }}>
                                        <button className="btn-edit" onClick={() => { setEdit(x.id); setF(x); setGpsStatus(null); }}>Edit</button>
                                        {x.status === 0 ? (
                                            <button className="btn-delete" onClick={() => openDeactivateModal(x)}>Deactivate</button>
                                        ) : (
                                            <button className="btn-success" onClick={() => activate(x.id)}>Activate</button>
                                        )}
                                    </div>
                                </td>
                            </tr>
                        )) : (
                            <tr>
                                <td colSpan="8" style={{ textAlign: "center", padding: "24px", color: "#94a3b8" }}>
                                    No microgrid nodes matched your search.
                                </td>
                            </tr>
                        )}
                    </tbody>
                </table>
            </Table>
        </div>

        {/* Deactivation Modal with Mandatory Admin Note & Suggestions */}
        {deactModal && (
            <div className="modal-overlay" onClick={() => setDeactModal(null)}>
                <div className="modal-card" onClick={e => e.stopPropagation()}>
                    <div className="modal-header">
                        <h2>
                            <AlertTriangle size={22}/>
                            Deactivate Microgrid Node
                        </h2>
                        <button className="modal-close-btn" onClick={() => setDeactModal(null)}>
                            <X size={20}/>
                        </button>
                    </div>

                    <div className="modal-info-box">
                        Deactivating <strong>{deactModal.nodeCode} — {deactModal.nodeName}</strong> will immediately set the hub to Inactive and automatically lock all related Energy Slots to <u>Unavailable</u>.
                    </div>

                    {modalErr && <div className="err" style={{ margin: "0 0 14px" }}>{modalErr}</div>}

                    <form onSubmit={confirmDeactivate}>
                        <span className="suggestion-chips-label">Quick Suggestions (Click to set reason):</span>
                        <div className="suggestion-chips">
                            {suggestions.map(s => (
                                <button
                                    key={s}
                                    type="button"
                                    className={`suggestion-chip ${deactReason === s ? "active" : ""}`}
                                    onClick={() => setDeactReason(s)}
                                >
                                    {s}
                                </button>
                            ))}
                        </div>

                        <label style={{ display: "block", marginTop: 0 }}>
                            <span style={{ fontWeight: 600, color: "#f8fafc" }}>Admin Note / Deactivation Reason * (Required):</span>
                            <textarea
                                required
                                rows={3}
                                style={{ width: "100%", marginTop: "8px", resize: "vertical" }}
                                placeholder="Enter reason (e.g., Routine Maintenance, Grid Repair, etc.)..."
                                value={deactReason}
                                onChange={e => setDeactReason(e.target.value)}
                            />
                        </label>

                        <div className="form-actions" style={{ marginTop: "20px" }}>
                            <button
                                type="submit"
                                className="btn-delete"
                                style={{ flex: 1, padding: "12px", fontSize: "14px", borderRadius: "12px" }}
                            >
                                Confirm Deactivation
                            </button>
                            <button
                                type="button"
                                onClick={() => setDeactModal(null)}
                                style={{ background: "#475569", flex: 1, padding: "12px", fontSize: "14px", borderRadius: "12px" }}
                            >
                                Cancel
                            </button>
                        </div>
                    </form>
                </div>
            </div>
        )}
    </>;
}

function Slots(){
    const empty = {nodeId:"",slotDate:"",startTime:"08:00",endTime:"09:00",energyAmountKwh:"",status:"Available"};
    const [f,setF]=useState(empty),[nodes,setN]=useState([]),[a,setA]=useState([]),[edit,setEdit]=useState(null),[err,setErr]=useState("");
    const [editApprovedKwh, setEditApprovedKwh] = useState(0);
    const [search, setSearch] = useState("");

    const load=()=>Promise.all([api.get("/microgrid-nodes"),api.get("/energy-slots")]).then(([n,s])=>{setN(n.data);setA(s.data)});
    useEffect(()=>{load();},[]);

    const selectedNode = nodes.find(n => n.id === f.nodeId);
    const isSelectedNodeInactive = selectedNode && selectedNode.status !== 0 && selectedNode.status !== "Active";

    const currentTotal = +f.energyAmountKwh || 0;
    const calculatedAvailable = edit 
        ? Math.max(0, +(currentTotal - editApprovedKwh).toFixed(2)) 
        : currentTotal;

    async function go(e){
        e.preventDefault();
        if (selectedNode && selectedNode.capacityKw) {
            if (+f.energyAmountKwh > selectedNode.capacityKw) {
                setErr(`Total energy (${f.energyAmountKwh} kWh) cannot exceed node capacity (${selectedNode.capacityKw} kW) for ${selectedNode.nodeCode} (${selectedNode.nodeName}).`);
                return;
            }
        }
        if (edit && +f.energyAmountKwh < editApprovedKwh) {
            setErr(`Total energy (${f.energyAmountKwh} kWh) cannot be less than already approved reservations (${editApprovedKwh} kWh).`);
            return;
        }
        if (edit && isSelectedNodeInactive && f.status === "Available") {
            setErr(`Cannot set slot status to Available because microgrid node '${selectedNode.nodeName}' is deactivated (${selectedNode.adminNote || "Inactive"}).`);
            return;
        }
        try {
            const b = {
                ...f, 
                energyAmountKwh: +f.energyAmountKwh, 
                slotDate: new Date(f.slotDate).toISOString()
            };
            if (edit) {
                b.availableCapacityKwh = +calculatedAvailable;
                b.status = calculatedAvailable <= 0 ? "Reserved" : f.status.toString();
                await api.put(`/energy-slots/${edit}`, b);
            } else {
                await api.post("/energy-slots", b);
            }
            setEdit(null); setF(empty); setEditApprovedKwh(0); setErr(""); load();
        } catch(x) { setErr(x.response?.data?.error || "Failed to save slot. Check inputs."); }
    }

    function startEdit(s) {
        const statuses = ["Available", "Reserved", "Unavailable", "Completed"];
        setEdit(s.id);
        const nodeForSlot = nodes.find(n => n.id === s.nodeId);
        const rawStatus = typeof s.status === 'number' ? statuses[s.status] : s.status;
        const approvedKwh = Math.max(0, +((s.energyAmountKwh - s.availableCapacityKwh).toFixed(2)));
        setEditApprovedKwh(approvedKwh);
        setF({
            nodeId: s.nodeId,
            slotDate: new Date(s.slotDate).toISOString().split('T')[0],
            startTime: s.startTime,
            endTime: s.endTime,
            energyAmountKwh: s.energyAmountKwh,
            status: (nodeForSlot && nodeForSlot.status !== 0 && nodeForSlot.status !== "Active" && rawStatus === "Available") ? "Unavailable" : rawStatus
        });
        setErr("");
    }

    const filteredSlots = a.filter(s => {
        if (!search.trim()) return true;
        const q = search.toLowerCase();
        const node = nodes.find(n => n.id === s.nodeId);
        return (
            node?.nodeCode?.toLowerCase().includes(q) ||
            node?.nodeName?.toLowerCase().includes(q) ||
            node?.adminNote?.toLowerCase().includes(q) ||
            s.startTime?.includes(q) ||
            s.endTime?.includes(q) ||
            new Date(s.slotDate).toLocaleDateString().includes(q)
        );
    });

    return <>
        <Title t="Energy Slots" d="Create and monitor trading slots"/>
        <div className="grid2">
            <form className="card" onSubmit={go}>
                <h2>{edit ? "Edit Energy Slot" : "Create Energy Slot"}</h2>
                {err && <div className="err">{err}</div>}

                {isSelectedNodeInactive && (
                    <div className="err" style={{ background: "rgba(245, 158, 11, 0.12)", borderColor: "rgba(245, 158, 11, 0.35)", color: "#fef08a" }}>
                        <AlertTriangle size={18} style={{ flexShrink: 0 }}/>
                        <div>
                            <strong>Branch Deactivated:</strong> {selectedNode.nodeCode} ({selectedNode.nodeName}) is currently inactive.
                            <div style={{ fontSize: "12px", marginTop: "3px" }}>
                                Reason: <em>"{selectedNode.adminNote || selectedNode.deactivationReason || "Routine Maintenance"}"</em>. Slots cannot be made Available.
                            </div>
                        </div>
                    </div>
                )}

                <label>Node
                    <select required disabled={!!edit} value={f.nodeId} onChange={e=>setF({...f,nodeId:e.target.value})}>
                        <option value="">Select Microgrid Node</option>
                        {nodes.map(n=>{
                            const isInactive = n.status !== 0 && n.status !== "Active";
                            return (
                                <option value={n.id} key={n.id} disabled={isInactive}>
                                    {n.nodeCode} — {n.nodeName} ({n.capacityKw} kW) {isInactive ? `⚠️ [Deactivated: ${n.adminNote || "Unavailable"}]` : ""}
                                </option>
                            );
                        })}
                    </select>
                </label>
                <label>Date<input required type="date" value={f.slotDate} onChange={e=>setF({...f,slotDate:e.target.value})}/></label>
                <label>Start<input type="time" required value={f.startTime} onChange={e=>setF({...f,startTime:e.target.value})}/></label>
                <label>End<input type="time" required value={f.endTime} onChange={e=>setF({...f,endTime:e.target.value})}/></label>
                <label>
                    <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                        <span>Total Energy (kWh)</span>
                        {selectedNode && (
                            <span style={{ fontSize: "12px", color: "var(--accent-primary)", fontWeight: 600 }}>
                                Max Capacity: {selectedNode.capacityKw} kW
                            </span>
                        )}
                    </div>
                    <input
                        required
                        type="number"
                        min={edit ? Math.max(0.1, editApprovedKwh) : 0.1}
                        step="0.1"
                        max={selectedNode ? selectedNode.capacityKw : undefined}
                        placeholder={selectedNode ? `Max ${selectedNode.capacityKw} kWh` : "Enter energy amount"}
                        value={f.energyAmountKwh}
                        onChange={e=>setF({...f,energyAmountKwh:e.target.value})}
                    />
                </label>
                {edit && (
                    <>
                        <label>
                            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                                <span>Available (kWh)</span>
                                <span style={{ fontSize: "11px", color: "var(--accent-primary)", fontWeight: 600 }}>
                                    ⚡ Auto-calculated (Total − Approved)
                                </span>
                            </div>
                            <input
                                type="number"
                                value={calculatedAvailable}
                                readOnly
                                disabled
                                style={{
                                    cursor: "not-allowed",
                                    opacity: 0.85,
                                    background: "rgba(255, 255, 255, 0.05)",
                                    borderColor: "rgba(255, 255, 255, 0.15)",
                                    fontWeight: 600,
                                    color: calculatedAvailable > 0 ? "var(--accent-primary)" : "#f87171"
                                }}
                                title="Available capacity is calculated automatically based on approved reservations."
                            />
                            <div style={{ fontSize: "12px", color: "#94a3b8", marginTop: "4px" }}>
                                {editApprovedKwh > 0 ? (
                                    <span>Locked by <strong>{editApprovedKwh} kWh</strong> in approved reservations.</span>
                                ) : (
                                    <span>No approved reservations yet. Full slot ({calculatedAvailable} kWh) is available.</span>
                                )}
                            </div>
                        </label>
                        <label>Status
                            <select
                                value={calculatedAvailable <= 0 ? "Reserved" : f.status}
                                disabled={isSelectedNodeInactive || calculatedAvailable <= 0}
                                onChange={e=>setF({...f,status:e.target.value})}
                            >
                                <option value="Available" disabled={isSelectedNodeInactive || calculatedAvailable <= 0}>
                                    Available {isSelectedNodeInactive ? "(Locked - Node Inactive)" : calculatedAvailable <= 0 ? "(Locked - Fully Reserved)" : ""}
                                </option>
                                <option value="Unavailable">Unavailable</option>
                                <option value="Reserved" disabled={calculatedAvailable > 0}>
                                    Reserved {calculatedAvailable > 0 ? "(Available capacity exists)" : ""}
                                </option>
                            </select>
                        </label>
                    </>
                )}
                <div className="form-actions">
                    <button type="submit" style={{flex:1}}>{edit ? "Update Slot" : "Create Slot"}</button>
                    {edit && <button type="button" onClick={() => { setEdit(null); setF(empty); setEditApprovedKwh(0); setErr(""); }} style={{background:"#94a3b8",flex:1}}>Cancel</button>}
                </div>
            </form>

            <Table>
                <div className="table-header">
                    <h2 style={{ margin: 0 }}>Energy Slots</h2>
                    <div className="table-search-bar">
                        <Search size={16}/>
                        <input
                            type="text"
                            placeholder="Filter by node, date..."
                            value={search}
                            onChange={e => setSearch(e.target.value)}
                        />
                        {search && (
                            <button type="button" onClick={() => setSearch("")} style={{ background: "transparent", border: "none", color: "#94a3b8", cursor: "pointer", padding: "0 4px", display: "flex", alignItems: "center" }}>
                                <X size={14}/>
                            </button>
                        )}
                    </div>
                </div>

                <table>
                    <thead>
                        <tr>
                            <th>Date</th>
                            <th>Node / Branch</th>
                            <th>Time</th>
                            <th>Total</th>
                            <th>Available</th>
                            <th>Status</th>
                            <th style={{ textAlign: "right", paddingRight: "18px" }}>Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        {filteredSlots.length > 0 ? filteredSlots.map(s => {
                            const node = nodes.find(n => n.id === s.nodeId);
                            const isNodeInactive = node && node.status !== 0 && node.status !== "Active";
                            const statusStr = typeof s.status === 'number' ? ["Available", "Reserved", "Unavailable", "Completed"][s.status] : s.status;
                            return (
                                <tr key={s.id}>
                                    <td>{new Date(s.slotDate).toLocaleDateString()}</td>
                                    <td>
                                        <div className="cell-branch-stack">
                                            <div className="branch-title-row">
                                                <strong>{node?.nodeCode || s.nodeId}</strong>
                                                {node?.nodeName && <span className="branch-subname">({node.nodeName})</span>}
                                            </div>
                                            {isNodeInactive && (
                                                <span className="branch-inactive-tag" title={`Reason: ${node.adminNote || "Routine Maintenance"}`}>
                                                    <AlertTriangle size={11} style={{ flexShrink: 0 }}/>
                                                    <span>{node.adminNote || "Node Deactivated"}</span>
                                                </span>
                                            )}
                                        </div>
                                    </td>
                                    <td>{s.startTime}–{s.endTime}</td>
                                    <td>{s.energyAmountKwh} kWh</td>
                                    <td>{s.availableCapacityKwh} kWh</td>
                                    <td>
                                        <span className={`badge ${statusStr === "Available" ? "badge-active" : "badge-inactive"}`}>
                                            {statusStr}
                                        </span>
                                    </td>
                                    <td style={{ textAlign: "right", paddingRight: "18px" }}>
                                        <button className="btn-edit" onClick={()=>startEdit(s)}>Edit</button>
                                    </td>
                                </tr>
                            );
                        }) : (
                            <tr>
                                <td colSpan="7" style={{ textAlign: "center", padding: "24px", color: "#94a3b8" }}>
                                    No energy slots found.
                                </td>
                            </tr>
                        )}
                    </tbody>
                </table>
            </Table>
        </div>
    </>;
}

function Res(){
    const [st, setSt] = useState("Pending"), [a, setA] = useState([]), [nodes, setN] = useState([]);
    const [search, setSearch] = useState("");
    const load = () => Promise.all([
        api.get(`/reservations/status/${st}`),
        api.get("/microgrid-nodes")
    ]).then(([r, n]) => {
        setA(r.data);
        setN(n.data);
    });
    useEffect(() => { load(); }, [st]);

    async function approve(r){
        try{
            await api.put(`/reservations/${r.id}/approve`);
            load();
        }catch(err){
            alert(err.response?.data?.error || "Failed to approve reservation.");
        }
    }

    const filtered = a.filter(r => {
        if (!search.trim()) return true;
        const q = search.toLowerCase();
        const node = nodes.find(n => n.id === r.nodeId || n.nodeCode === r.nodeId);
        return (
            r.reservationCode?.toLowerCase().includes(q) ||
            node?.nodeCode?.toLowerCase().includes(q) ||
            node?.nodeName?.toLowerCase().includes(q) ||
            r.nodeCode?.toLowerCase().includes(q) ||
            r.nodeName?.toLowerCase().includes(q) ||
            r.nodeId?.toLowerCase().includes(q) ||
            r.startTime?.includes(q) ||
            r.endTime?.includes(q) ||
            new Date(r.reservationDate).toLocaleDateString().includes(q)
        );
    });

    return <>
        <Title t="Reservations" d="Monitor and approve reservation workflow"/>
        <Table>
            <div className="table-header" style={{ marginBottom: "16px" }}>
                <div className="tabs" style={{ margin: 0 }}>
                    {["Pending","Approved","Completed","Cancelled"].map(x => (
                        <button className={st === x ? "selected" : ""} onClick={() => setSt(x)} key={x}>
                            {x}
                        </button>
                    ))}
                </div>
                <div className="table-search-bar">
                    <Search size={16}/>
                    <input
                        type="text"
                        placeholder="Filter by code, node, date..."
                        value={search}
                        onChange={e => setSearch(e.target.value)}
                    />
                    {search && (
                        <button type="button" onClick={() => setSearch("")} style={{ background: "transparent", border: "none", color: "#94a3b8", cursor: "pointer", padding: "0 4px", display: "flex", alignItems: "center" }}>
                            <X size={14}/>
                        </button>
                    )}
                </div>
            </div>
            <table>
                <thead>
                    <tr>
                        <th>Code</th>
                        <th>Node / Branch</th>
                        <th>Date</th>
                        <th>Time</th>
                        <th>Energy</th>
                        <th>Status</th>
                        {st === "Pending" && <th style={{ textAlign: "right", paddingRight: "18px" }}>Action</th>}
                    </tr>
                </thead>
                <tbody>
                    {filtered.length > 0 ? (
                        filtered.map(r => {
                            const node = nodes.find(n => n.id === r.nodeId || n.nodeCode === r.nodeId);
                            const nodeCode = node?.nodeCode || r.nodeCode || r.nodeId;
                            const nodeName = node?.nodeName || r.nodeName;
                            const badgeClass = 
                                r.status === "Approved" ? "badge-active" : 
                                r.status === "Completed" ? "badge-completed" : 
                                r.status === "Cancelled" ? "badge-inactive" : "badge-pending";

                            return (
                                <tr key={r.id}>
                                    <td><strong>{r.reservationCode}</strong></td>
                                    <td>
                                        <div className="cell-branch-stack">
                                            <div className="branch-title-row">
                                                <strong>{nodeCode}</strong>
                                                {nodeName && <span className="branch-subname">({nodeName})</span>}
                                            </div>
                                        </div>
                                    </td>
                                    <td>{new Date(r.reservationDate).toLocaleDateString()}</td>
                                    <td>{r.startTime}–{r.endTime}</td>
                                    <td>{r.energyAmountKwh} kWh</td>
                                    <td>
                                        <span className={`badge ${badgeClass}`}>
                                            {r.status}
                                        </span>
                                    </td>
                                    {st === "Pending" && (
                                        <td style={{ textAlign: "right", paddingRight: "18px" }}>
                                            <button className="btn-success" onClick={() => approve(r)}>Approve</button>
                                        </td>
                                    )}
                                </tr>
                            );
                        })
                    ) : (
                        <tr>
                            <td colSpan={st === "Pending" ? 7 : 6} style={{ textAlign: "center", padding: "28px", color: "#94a3b8" }}>
                                No {st.toLowerCase()} reservations found.
                            </td>
                        </tr>
                    )}
                </tbody>
            </table>
        </Table>
    </>;
}

function OpHome(){const[d,setD]=useState({});useEffect(()=>{api.get("/dashboard/operator").then(x=>setD(x.data))},[]);return <><Title t="Grid Operator Dashboard" d="Operational station monitoring"/><div className="stats"><Stat t="Pending" v={d.pendingReservations} I={CalendarDays}/><Stat t="Approved" v={d.approvedReservations} I={ShieldCheck}/><Stat t="Today's Bookings" v={d.todayBookings} I={Activity}/><Stat t="Available Slots" v={d.availableSlots} I={Battery}/></div></>}
function Bookings(){
    const [st, setSt] = useState("Approved"), [a, setA] = useState([]), [nodes, setN] = useState([]);
    const load = () => Promise.all([
        api.get(`/reservations/status/${st}`),
        api.get("/microgrid-nodes")
    ]).then(([r, n]) => {
        setA(r.data);
        setN(n.data);
    });
    useEffect(() => { load(); }, [st]);
    return <>
        <Title t="Bookings" d="View pending and approved reservations"/>
        <Table>
            <div className="tabs">
                <button className={st === "Pending" ? "selected" : ""} onClick={() => setSt("Pending")}>Pending</button>
                <button className={st === "Approved" ? "selected" : ""} onClick={() => setSt("Approved")}>Approved</button>
            </div>
            <table>
                <thead>
                    <tr>
                        <th>Code</th>
                        <th>Node / Branch</th>
                        <th>Date</th>
                        <th>Time</th>
                        <th>Energy</th>
                    </tr>
                </thead>
                <tbody>
                    {a.length > 0 ? (
                        a.map(r => {
                            const node = nodes.find(n => n.id === r.nodeId || n.nodeCode === r.nodeId);
                            const nodeCode = node?.nodeCode || r.nodeCode || r.nodeId;
                            const nodeName = node?.nodeName || r.nodeName;
                            return (
                                <tr key={r.id}>
                                    <td><strong>{r.reservationCode}</strong></td>
                                    <td>
                                        <div className="cell-branch-stack">
                                            <div className="branch-title-row">
                                                <strong>{nodeCode}</strong>
                                                {nodeName && <span className="branch-subname">({nodeName})</span>}
                                            </div>
                                        </div>
                                    </td>
                                    <td>{new Date(r.reservationDate).toLocaleDateString()}</td>
                                    <td>{r.startTime}–{r.endTime}</td>
                                    <td>{r.energyAmountKwh} kWh</td>
                                </tr>
                            );
                        })
                    ) : (
                        <tr>
                            <td colSpan="5" style={{ textAlign: "center", padding: "28px", color: "#94a3b8" }}>
                                No {st.toLowerCase()} bookings found.
                            </td>
                        </tr>
                    )}
                </tbody>
            </table>
        </Table>
    </>;
}
function Avail(){
    const [a, setA] = useState([]), [nodes, setN] = useState([]);
    const load = () => Promise.all([api.get("/energy-slots"), api.get("/microgrid-nodes")]).then(([s, n]) => { setA(s.data); setN(n.data); });
    useEffect(() => { load(); }, []);
    async function up(s) {
        const node = nodes.find(n => n.id === s.nodeId);
        const maxAllowed = node?.capacityKw ? Math.min(s.energyAmountKwh, node.capacityKw) : s.energyAmountKwh;
        const v = prompt(`Available capacity (Max: ${maxAllowed} kWh):`, s.availableCapacityKwh);
        if (v != null) {
            const num = parseFloat(v);
            if (isNaN(num) || num < 0 || num > maxAllowed) {
                alert(`Invalid available capacity. Must be between 0 and ${maxAllowed} kWh (Node Capacity: ${node?.capacityKw || 'N/A'} kW, Slot Total: ${s.energyAmountKwh} kWh).`);
                return;
            }
            try {
                await api.put(`/energy-slots/${s.id}/availability?availableCapacityKwh=${num}`);
                load();
            } catch (err) {
                alert(err.response?.data?.error || "Failed to update availability");
            }
        }
    }
    return <><Title t="Energy Availability" d="Update battery and energy slot availability"/><Table><table><thead><tr><th>Slot</th><th>Total</th><th>Available</th><th>Status</th><th/></tr></thead><tbody>{a.map(s=><tr key={s.id}><td>{s.startTime}–{s.endTime}</td><td>{s.energyAmountKwh}</td><td>{s.availableCapacityKwh}</td><td>{s.status}</td><td><button className="btn-edit" onClick={()=>up(s)}>Update</button></td></tr>)}</tbody></table></Table></>;
}
function QR(){const[t,setT]=useState(""),[r,setR]=useState(null),[e,setE]=useState("");async function verify(){try{const x=await api.post("/qr/verify",{qrToken:t});setR(x.data);setE("")}catch(x){setE(x.response?.data?.error||"Invalid QR")}}async function complete(){try{const x=await api.post("/qr/complete",{qrToken:t});setR(x.data)}catch(x){setE(x.response?.data?.error||"Cannot complete")}}return <><Title t="QR Transaction Verification" d="Verify and finalize Prosumer energy transfers"/><div className="card qr"><QrCode size={48}/><textarea placeholder="Paste scanned QR token" value={t} onChange={e=>setT(e.target.value)}/>{e&&<div className="err">{e}</div>}<div className="qr-actions"><button onClick={verify}>Verify Transaction</button>{r?.valid&&<button onClick={complete}>Complete Transfer</button>}</div>{r&&<pre>{JSON.stringify(r,null,2)}</pre>}</div></>}

export default function App(){return <Routes><Route path="/login" element={<Login/>}/><Route path="/backoffice/*" element={<Guard role="Backoffice"/>}/><Route path="/operator/*" element={<Guard role="GridOperator"/>}/><Route path="*" element={<Navigate to="/login"/>}/></Routes>}