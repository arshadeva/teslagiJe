// Ocalix by Oscar Mandala - tanpa dependency, cukup: node index.js
const zlib=require('zlib'),http=require('http'),https=require('https'),fs=require('fs'),path=require('path'),{URL}=require('url');
const C=require('./config.js'),PUB=path.join(__dirname,'public'),cache=new Map(),ic=new Map(),sf=new Map(),fl=new Map();
const MIME={'.html':'text/html; charset=utf-8','.js':'text/javascript','.css':'text/css','.png':'image/png','.svg':'image/svg+xml','.json':'application/json'};
const UA='Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36';
function get(u,h={},body,n=0){return new Promise((ok,no)=>{
  const hd={'User-Agent':UA,...h};if(body)hd['Content-Length']=Buffer.byteLength(body);
  const r=https.request(u,{method:body?'POST':'GET',headers:hd},res=>{
    if(res.statusCode>=300&&res.statusCode<400&&res.headers.location&&n<3){res.resume();return ok(get(new URL(res.headers.location,u).href,h,null,n+1))}
    const b=[];res.on('data',d=>b.push(d));res.on('end',()=>ok({code:res.statusCode,type:res.headers['content-type'],buf:Buffer.concat(b)}))});
  r.setTimeout(12000,()=>r.destroy(new Error('timeout')));r.on('error',no);if(body)r.write(body);r.end()})}
async function yt(q){
  const body=JSON.stringify({context:{client:{clientName:'WEB',clientVersion:'2.20240726.00.00',hl:'id',gl:'ID'}},query:q,params:'EgIQAQ=='});
  const r=await get('https://www.youtube.com/youtubei/v1/search?prettyPrint=false&key=AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8',{'Content-Type':'application/json','Origin':'https://www.youtube.com'},body);
  const j=JSON.parse(r.buf.toString()),out=[];
  const secs=j.contents.twoColumnSearchResultsRenderer.primaryContents.sectionListRenderer.contents;
  for(const s of secs)for(const i of((s.itemSectionRenderer&&s.itemSectionRenderer.contents)||[])){
    const v=i.videoRenderer;if(!v||!v.lengthText||!v.lengthText.simpleText)continue;
    const d=v.lengthText.simpleText.split(':').reverse().reduce((a,x,k)=>a+x*Math.pow(60,k),0);if(d>1500)continue;
    out.push({id:v.videoId,title:v.title.runs.map(x=>x.text).join(''),artist:((v.ownerText&&v.ownerText.runs[0].text)||'').replace(/ - Topic$/,''),dur:d,cover:'https://i.ytimg.com/vi/'+v.videoId+'/hqdefault.jpg'})}
  return out}
async function itunes(q){const r=await get('https://itunes.apple.com/search?entity=song&limit=25&term='+encodeURIComponent(q));
  return JSON.parse(r.buf.toString()).results.map(x=>({id:'it'+x.trackId,title:x.trackName,artist:x.artistName,dur:30,cover:x.artworkUrl100.replace('100x100','600x600'),src:x.previewUrl}))}
function sr(q){const c=cache.get(q);if(c&&Date.now()-c.t<6e5)return Promise.resolve(c.d);if(fl.has(q))return fl.get(q);
  const p=(async()=>{let items=[],src='youtube';try{items=await yt(q)}catch(e){console.log('[yt]',e.message)}
    if(!items.length){src='preview';try{items=await itunes(q)}catch(e){console.log('[itunes]',e.message)}}
    const d={items,src};if(items.length){cache.set(q,{t:Date.now(),d});if(cache.size>200)cache.delete(cache.keys().next().value)}return d})().finally(()=>fl.delete(q));
  fl.set(q,p);return p}
const LY=require('./lyrics.js')(get);
const VC=[{n:101,ua:'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.5 Safari/605.1.15',c:{clientName:'VISIONOS',clientVersion:'1.02',deviceMake:'Apple',deviceModel:'RealityDevice17,1',osName:'visionOS',osVersion:'26.5.23O471'}},
 {n:3,ua:'com.google.android.youtube/20.10.38 (Linux; U; Android 11) gzip',c:{clientName:'ANDROID',clientVersion:'20.10.38',androidSdkVersion:30,osName:'Android',osVersion:'11'}}];
const ac=new Map(),av=new Map();
function pick(j){const f=((j.streamingData||{}).adaptiveFormats||[]).filter(x=>x.url&&/^audio\//.test(x.mimeType||''));
  const s=x=>x.itag===140?0:/mp4/.test(x.mimeType)?1:2;f.sort((a,b)=>s(a)-s(b));return f[0]}
const PURL='https://www.youtube.com/youtubei/v1/player?prettyPrint=false&key=AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8';
async function pl(v,id){const r=await get(PURL,{'Content-Type':'application/json','User-Agent':v.ua,'Origin':'https://www.youtube.com','X-YouTube-Client-Name':String(v.n),'X-YouTube-Client-Version':v.c.clientVersion},JSON.stringify({context:{client:Object.assign({hl:'en',gl:'US'},v.c)},videoId:id,contentCheckOk:true,racyCheckOk:true}));return JSON.parse(r.buf.toString())}
const okh=f=>f&&new URL(f.url).hostname.endsWith('.googlevideo.com');
const PS0=['https://pipedapi.kavin.rocks','https://pipedapi.leptons.xyz','https://pipedapi.nosebs.ru','https://pipedapi-libre.kavin.rocks','https://piped-api.privacy.com.de','https://pipedapi.adminforge.de','https://api.piped.yt','https://pipedapi.drgns.space','https://pipedapi.owo.si','https://pipedapi.ducks.party','https://piped-api.codespace.cz','https://pipedapi.reallyaweso.me','https://api.piped.private.coffee','https://pipedapi.darkness.services','https://pipedapi.orangenet.cc'];
let PL={t:0,l:[]},goodP='',vcSkip=0;const neg=new Map();
const first=ps=>new Promise(ok=>{let n=ps.length;if(!n)ok(null);ps.forEach(p=>p.then(v=>{if(v)ok(v);else if(!--n)ok(null)},()=>{if(!--n)ok(null)}))});
const okp=u=>{try{const x=new URL(u);return x.protocol==='https:'&&!/^(localhost$|\d+\.\d+\.\d+\.\d+$|\[)/.test(x.hostname)}catch(e){return false}};
async function plist(){if(Date.now()-PL.t<36e5&&PL.l.length)return PL.l;let d=[];
  try{const r=await get('https://raw.githubusercontent.com/TeamPiped/Documentation/main/content/docs/public-instances/index.md');d=(r.buf.toString().match(/\|\s*https:\/\/[^\s|]+\s*\|/g)||[]).map(x=>x.replace(/[|\s]/g,''))}catch(e){}
  const l=[].concat(C.PIPED||[],goodP?[goodP]:[],d,PS0).filter((x,i,a)=>a.indexOf(x)===i);PL={t:Date.now(),l};return l}
async function pp(b,id){const r=await Promise.race([get(b+'/streams/'+id,{Accept:'application/json'}),new Promise((_,no)=>setTimeout(()=>no(new Error('timeout')),6000))]);
  if(r.code!==200)throw new Error('http '+r.code);let j;try{j=JSON.parse(r.buf.toString())}catch(e){throw new Error('diblokir/bukan JSON')}
  const sc=x=>x.bitrate>192000?1e9:-(x.bitrate||0),f=(j.audioStreams||[]).filter(x=>x.url&&!x.videoOnly&&/mp4/.test(x.mimeType||'')&&okp(x.url)).sort((a,b)=>sc(a)-sc(b))[0];
  if(!f)throw new Error('tanpa stream audio');return{u:f.url,ua:UA,m:'audio/mp4',t:Date.now(),s:b}}
const wy=new Map();let lastPE='';
async function piped(id){if(C.PIPED===false)return null;const l=await plist(),PE={};
  for(let i=0;i<l.length&&i<8;i+=4){const o=await first(l.slice(i,i+4).map(b=>pp(b,id).catch(e=>{PE[e.message]=(PE[e.message]||0)+1;throw e})));if(o){goodP=o.s;return o}}
  lastPE=Object.entries(PE).map(([k,v])=>k+' x'+v).join(', ')||'tidak ada instance';return null}
function rv(id,fresh){const c=ac.get(id);if(!fresh&&c&&Date.now()-c.t<72e5)return Promise.resolve(c);if(!fresh&&neg.get(id)>Date.now())return Promise.resolve(null);if(av.has(id))return av.get(id);
  const p=(async()=>{let o=null;const W=[];
    if(Date.now()>vcSkip){for(const v of VC){try{const j=await pl(v,id),f=pick(j);
      if(okh(f)){o={u:f.url,ua:v.ua,m:f.mimeType.split(';')[0],t:Date.now(),s:v.c.clientName};break}
      const ps=j.playabilityStatus||{};W.push(v.c.clientName+' '+(ps.status||'?')+(ps.reason?' ('+ps.reason.slice(0,45)+')':''));console.log('[audio]',v.c.clientName,ps.status||'?',ps.reason||'')}catch(e){W.push(v.c.clientName+' '+e.message);console.log('[audio]',v.c.clientName,e.message)}}
      if(o)vcSkip=0;else{vcSkip=Date.now()+3e5;console.log('[audio] YouTube langsung gagal, coba Piped')}}else W.push('YouTube dilewati (baru gagal)');
    if(!o){o=await piped(id).catch(()=>null);if(o)console.log('[audio] via Piped '+o.s);else{W.push('Piped: '+lastPE);console.log('[audio] semua jalur gagal')}}
    wy.set(id,o?'ok':W.join(' | '));if(wy.size>100)wy.delete(wy.keys().next().value);
    if(o){ac.set(id,o);if(ac.size>300)ac.delete(ac.keys().next().value)}else neg.set(id,Date.now()+6e4);return o})().finally(()=>av.delete(id));av.set(id,p);return p}
async function dbg(id){const L=[];let ok=false;
  for(const v of VC){const n=v.c.clientName;try{const j=await pl(v,id),ps=j.playabilityStatus||{},f=((j.streamingData||{}).adaptiveFormats||[]).filter(x=>/^audio\//.test(x.mimeType||'')),p=pick(j);
    let s=n+': '+(ps.status||'?')+(ps.reason?' ('+ps.reason+')':'')+' | audio '+f.length+', ber-URL '+f.filter(x=>x.url).length;
    if(okh(p)){const q=await get(p.url,{'User-Agent':v.ua,Range:'bytes=2000000-2000099'}).then(r=>r.code,e=>e.message);s+=' | tes byte setelah 1MB: '+q;if(q===206||q===200)ok=true}L.push(s)}catch(e){L.push(n+': error '+e.message)}}
  if(C.PIPED!==false){const l=await plist(),rs=await Promise.all(l.slice(0,8).map(b=>pp(b,id).then(v=>{L.push('Piped '+b.slice(8)+': OK');return v},e=>{L.push('Piped '+b.slice(8)+': '+e.message);return null})));
    const pv=rs.find(Boolean);if(pv){const q=await get(pv.u,{'User-Agent':UA,Range:'bytes=2000000-2000099'}).then(r=>r.code,e=>e.message);L.push('Piped tes byte: '+q);if(q===206||q===200)ok=true}}
  return{ok,lines:L}}
async function audio(req,res,u,retry){const id=u.searchParams.get('id')||'';
  if(C.AUDIO===false||!/^[\w-]{11}$/.test(id)){res.writeHead(404);return res.end()}
  if(u.searchParams.get('dbg'))return J(res,await dbg(id));
  const s=await rv(id,retry||u.searchParams.get('fresh')==='1');if(!s){res.writeHead(502);return res.end()}
  const up=https.get(s.u,{headers:{'User-Agent':s.ua,Range:req.headers.range||'bytes=0-'}},r=>{up.setTimeout(0);
    if(r.statusCode>=400){r.resume();ac.delete(id);if(!retry)return audio(req,res,u,1);res.writeHead(502);return res.end()}
    const h={'Content-Type':s.m,'Accept-Ranges':'bytes','Cache-Control':'no-store'};
    ['content-length','content-range'].forEach(k=>{if(r.headers[k])h[k]=r.headers[k]});res.writeHead(r.statusCode,h);r.pipe(res)});
  up.setTimeout(15000,()=>up.destroy());up.on('error',()=>{try{if(!res.headersSent)res.writeHead(502);res.end()}catch(e){}});res.on('close',()=>up.destroy())}
const HOME=['lagu indonesia terpopuler 2026','lagu santai akustik indonesia','top global hits 2026'];
const warm=()=>HOME.forEach((q,i)=>setTimeout(()=>sr(q).catch(()=>{}),i*800));
const J=(res,o)=>{res.writeHead(200,{'Content-Type':'application/json'});res.end(JSON.stringify(o))};
http.createServer(async(req,res)=>{try{
  const u=new URL(req.url,'http://x'),p=u.pathname;
  if(p==='/api/config')return J(res,{name:'Ocalix',version:'1.3.0',domain:C.DOMAIN,port:C.PORT,lyrics:true});
  if(p==='/api/lyrics'){const ti=(u.searchParams.get('title')||'').trim().slice(0,200),ar=(u.searchParams.get('artist')||'').trim().slice(0,120);
    if(!ti)return J(res,{synced:[],plain:'',src:''});return J(res,await LY.find(ti,ar,+u.searchParams.get('dur')||0,u.searchParams.get('fresh')==='1'))}
  if(p==='/api/search'){const q=(u.searchParams.get('q')||'').trim().slice(0,100);if(!q)return J(res,{items:[]});
    return J(res,await sr(q))}
  if(p==='/api/why')return J(res,{v:'1.3.0',why:wy.get(u.searchParams.get('id')||'')||'server belum sempat mencoba'});
  if(p==='/api/audio')return await audio(req,res,u);
  if(p==='/api/img'){const t=new URL(u.searchParams.get('u')||'x:');
    if(t.protocol!=='https:'||!/(^|\.)(ytimg\.com|mzstatic\.com|ggpht\.com|googleusercontent\.com)$/.test(t.hostname)){res.writeHead(403);return res.end()}
    let r=ic.get(t.href);if(!r){r=await get(t.href);if(r.code===200){ic.set(t.href,r);if(ic.size>80)ic.delete(ic.keys().next().value)}}res.writeHead(r.code,{'Content-Type':r.type||'image/jpeg','Cache-Control':'public,max-age=86400'});return res.end(r.buf)}
  const f=path.normalize(path.join(PUB,p==='/'?'index.html':decodeURIComponent(p)));
  if(!f.startsWith(PUB)){res.writeHead(404);return res.end('404')}
  let st;try{st=fs.statSync(f);if(st.isDirectory())throw 0}catch(e){res.writeHead(404);return res.end('404')}
  let s=sf.get(f);if(!s||s.m!==st.mtimeMs){const b=fs.readFileSync(f),x=path.extname(f);s={m:st.mtimeMs,b,z:/^\.(html|js|css|svg|json)$/.test(x)?zlib.gzipSync(b,{level:9}):null,t:MIME[x]||'application/octet-stream',e:'"'+st.mtimeMs.toString(36)+'-'+b.length.toString(36)+'"',c:x==='.html'?'no-cache':'public,max-age=86400'};sf.set(f,s)}
  const hd={'Content-Type':s.t,'Cache-Control':s.c,ETag:s.e};
  if(req.headers['if-none-match']===s.e){res.writeHead(304,hd);return res.end()}
  const gz=s.z&&/gzip/.test(req.headers['accept-encoding']||''),o=gz?s.z:s.b;if(gz)hd['Content-Encoding']='gzip';hd['Content-Length']=o.length;res.writeHead(200,hd);res.end(o)
}catch(e){try{res.writeHead(500);res.end('error')}catch(_){}}})
.on('error',e=>console.log('Gagal start:',e.message,'-> cek PORT di config.js / Allocation panel'))
.listen(C.PORT,C.HOST,()=>{console.log('Ocalix aktif -> http://'+C.DOMAIN+':'+C.PORT+'  (by Oscar Mandala)');warm();setInterval(warm,66e4)});
process.on('uncaughtException',e=>console.log('[err]',e.message));
