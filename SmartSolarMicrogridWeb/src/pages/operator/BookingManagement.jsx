import { useEffect, useRef, useState } from "react";
import { Sun, LayoutDashboard, Users, MapPin, Battery, CalendarDays, QrCode, LogOut, Menu, X, ShieldCheck, Activity, UserPlus, Eye, EyeOff, AlertTriangle, Search, Navigation, Crosshair, ExternalLink, CheckCircle2, Loader2, Map, Compass, Check, AlertCircle } from "lucide-react";
import Title from "../../components/common/PageTitle";
import Table from "../../components/common/DataTable";
import * as bookingService from "../../services/bookingService";

export default function Bookings(){const[st,setSt]=useState("Approved"),[a,setA]=useState([]);useEffect(()=>{bookingService.getReservations(st).then(x=>setA(x.data))},[st]);return <><Title t="Bookings" d="View pending and approved reservations"/><Table><div className="tabs"><button className={st==="Pending"?"selected":""} onClick={()=>setSt("Pending")}>Pending</button><button className={st==="Approved"?"selected":""} onClick={()=>setSt("Approved")}>Approved</button></div><table><thead><tr><th>Code</th><th>Node</th><th>Date</th><th>Time</th><th>Energy</th></tr></thead><tbody>{a.map(r=><tr key={r.id}><td>{r.reservationCode}</td><td>{r.nodeId}</td><td>{new Date(r.reservationDate).toLocaleDateString()}</td><td>{r.startTime}–{r.endTime}</td><td>{r.energyAmountKwh}</td></tr>)}</tbody></table></Table></>}
