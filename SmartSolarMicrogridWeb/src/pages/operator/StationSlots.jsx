import { useEffect, useRef, useState } from "react";
import { Sun, LayoutDashboard, Users, MapPin, Battery, CalendarDays, QrCode, LogOut, Menu, X, ShieldCheck, Activity, UserPlus, Eye, EyeOff, AlertTriangle, Search, Navigation, Crosshair, ExternalLink, CheckCircle2, Loader2, Map, Compass, Check, AlertCircle } from "lucide-react";
import Title from "../../components/common/PageTitle";
import Table from "../../components/common/DataTable";
import * as bookingService from "../../services/bookingService";

export default function Avail(){const[a,setA]=useState([]);const load=()=>bookingService.getEnergySlots().then(x=>setA(x.data));useEffect(()=>{load();},[]);async function up(s){const v=prompt("Available capacity",s.availableCapacityKwh);if(v!=null){await bookingService.updateSlotAvailability(s.id,v);load()}}return <><Title t="Energy Availability" d="Update battery and energy slot availability"/><Table><table><thead><tr><th>Slot</th><th>Total</th><th>Available</th><th>Status</th><th/></tr></thead><tbody>{a.map(s=><tr key={s.id}><td>{s.startTime}–{s.endTime}</td><td>{s.energyAmountKwh}</td><td>{s.availableCapacityKwh}</td><td>{s.status}</td><td><button className="btn-edit" onClick={()=>up(s)}>Update</button></td></tr>)}</tbody></table></Table></>}
