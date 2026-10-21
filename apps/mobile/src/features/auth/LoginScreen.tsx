import { useState } from 'react';
import { View, Text, TextInput, Button } from 'react-native';
import { login, type AccountSession } from './session';
export function LoginScreen({ onLogin }: { onLogin:(session:AccountSession)=>void }) {
 const [email,setEmail]=useState(''),[password,setPassword]=useState(''),[message,setMessage]=useState(''),[busy,setBusy]=useState(false);
 async function submit(){setBusy(true);setMessage('');try{const session=await login(email,password);setPassword('');onLogin(session);}catch(e){setMessage(e instanceof Error?e.message:'No hay conexión. Reintenta.');}finally{setBusy(false);}}
 return <View><Text accessibilityRole="header">Ingresar con credenciales</Text><Text>Correo</Text><TextInput accessibilityLabel="Correo" value={email} onChangeText={setEmail} autoCapitalize="none" keyboardType="email-address" autoComplete="username"/><Text>Contraseña</Text><TextInput accessibilityLabel="Contraseña" value={password} onChangeText={setPassword} secureTextEntry autoComplete="password"/><Button title={busy?'Ingresando…':'Ingresar'} disabled={busy} onPress={()=>{void submit();}}/>{message!==''&&<Text accessibilityRole="alert">{message}</Text>}</View>;
}
