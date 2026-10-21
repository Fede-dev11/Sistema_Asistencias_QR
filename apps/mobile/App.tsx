import { useEffect, useState } from 'react';
import { SafeAreaView, Text, Button, StyleSheet } from 'react-native';
import { LoginScreen } from './src/features/auth/LoginScreen';
import { onUnauthorized } from './src/services/api';
import { clear, restore, logout, type AccountSession } from './src/features/auth/session';
import { AttendanceScreen } from './src/features/attendance/AttendanceScreen';
import { clearGuestResume } from './src/features/attendance/guestResume';
export default function App() {
 const [session,setSession]=useState<AccountSession|null>(null),[message,setMessage]=useState(''),[loginVisible,setLoginVisible]=useState(false);
 useEffect(()=>onUnauthorized(()=>{setSession(null);void Promise.all([clear(),clearGuestResume()]).catch(()=>setMessage('Ingresa nuevamente. No se pudo retirar la sesión guardada.'));}),[]);
 useEffect(()=>{void restore().then(setSession).catch(()=>setMessage('No se pudo recuperar la sesión. Ingresa nuevamente.'));},[]);
 async function exit(){try{await logout();}catch{setMessage('Se cerró la sesión local; falta confirmar la revocación en el servidor.');}finally{await clearGuestResume();setSession(null);}}
 return <SafeAreaView style={styles.root}><Text accessibilityRole="header" style={styles.title}>UNL GeoAsist</Text>{session?<><Text accessibilityRole="summary">Acceso con credenciales confirmado.</Text><Button title="Cerrar sesión" onPress={()=>{void exit();}}/></>:loginVisible?<><LoginScreen onLogin={value=>{setSession(value);setLoginVisible(false);}}/><Button title="Volver al escáner" onPress={()=>setLoginVisible(false)}/></>:<Button title="Ingresar con credenciales" onPress={()=>setLoginVisible(true)}/>} {!loginVisible&&<AttendanceScreen session={session}/>} {message!==''&&<Text accessibilityRole="alert">{message}</Text>}</SafeAreaView>;
}
const styles=StyleSheet.create({root:{flex:1,padding:24,backgroundColor:'#f5f6f1'},title:{fontSize:30,fontWeight:'700',color:'#18382f'}});
