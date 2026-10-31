package com.raufpnl.app

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.InputType
import android.view.Gravity
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : Activity() {
    private val bg = Color.rgb(9,9,12); private val panel = Color.rgb(21,21,27); private val red = Color.rgb(255,52,79); private val muted = Color.rgb(151,153,164)
    private lateinit var prefs: android.content.SharedPreferences
    private lateinit var body: LinearLayout
    private val tabs = listOf("Home","Sensitivity","DPI","Fire Button","Weapons","Headshot","Profiles")
    private val weapons = listOf("Desert Eagle","Woodpecker","UMP","M1887","SVD","AWM")
    private val sens = linkedMapOf("General" to 100,"Red Dot" to 95,"2X Scope" to 90,"4X Scope" to 85,"Sniper Scope" to 70,"Free Camera" to 100)
    private val dpi = mutableListOf<Pair<String,Int>>()
    private val fire = mutableListOf<Pair<String,Int>>()
    private val profiles = mutableListOf<String>()
    private val snapshots = mutableMapOf<String, JSONObject>()
    private val notes = mutableMapOf<String,String>()
    private val history = mutableListOf<Pair<Int,Int>>()
    private var selectedDpi = 0; private var fireSize = 48

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState); window.statusBarColor=bg; window.navigationBarColor=bg
        prefs=getSharedPreferences("rauf_pnl",MODE_PRIVATE); load()
        val root=LinearLayout(this).apply { orientation=1; setBackgroundColor(bg) }
        val head=LinearLayout(this).apply { orientation=0; gravity=Gravity.CENTER_VERTICAL; setPadding(d(18),d(14),d(18),d(10)) }
        val logo=TextView(this).apply { text="R"; textSize=22f; typeface=Typeface.DEFAULT_BOLD; gravity=Gravity.CENTER; setTextColor(-1); background=shape(red,14); }
        head.addView(logo,LinearLayout.LayoutParams(d(44),d(44)))
        val title=LinearLayout(this).apply { orientation=1; setPadding(d(12),0,0,0) }
        text(title,"RAUF PNL",18f,-1,true); text(title,"TRAINING CONSOLE",9f,red,true); head.addView(title,LinearLayout.LayoutParams(0,-2,1f))
        text(head,"● LIVE",10f,red,true); root.addView(head)
        val hs=HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled=false }
        val nav=LinearLayout(this).apply { orientation=0; setPadding(d(12),d(5),d(12),d(8)) }
        tabs.forEach { t -> val b=TextView(this).apply { text=t; textSize=12f; setTextColor(-1); gravity=Gravity.CENTER; setPadding(d(13),d(10),d(13),d(10)); background=shape(panel,14); setOnClickListener { page(t) } }; nav.addView(b,LinearLayout.LayoutParams(-2,-2).apply { rightMargin=d(7) }) }
        hs.addView(nav); root.addView(hs)
        body=LinearLayout(this).apply { orientation=1; setPadding(d(18),d(6),d(18),d(25)) }
        root.addView(ScrollView(this).apply { addView(body) },LinearLayout.LayoutParams(-1,0,1f))
        setContentView(root); page("Home")
    }
    private fun load() {
        try { val j=JSONObject(prefs.getString("sens","{}")); sens.keys.toList().forEach { sens[it]=j.optInt(it,sens[it]!!) } } catch (_:Exception) {}
        fun read(key:String, defaults:List<Pair<String,Int>>):MutableList<Pair<String,Int>> {
            val out=mutableListOf<Pair<String,Int>>()
            try { val a=JSONArray(prefs.getString(key,"[]")); for(i in 0 until a.length()) { val o=a.getJSONObject(i); out.add(o.getString("n") to o.getInt("v")) } } catch (_:Exception) {}
            if(out.isEmpty()) out.addAll(defaults); return out
        }
        dpi.addAll(read("dpi",listOf("Balanced" to 420,"Precision" to 480))); fire.addAll(read("fire",listOf("Default" to 48)))
        selectedDpi=prefs.getInt("sel",0).coerceIn(0,dpi.lastIndex); fireSize=prefs.getInt("size",48).coerceIn(10,100)
        try { val a=JSONArray(prefs.getString("profiles","[]")); for(i in 0 until a.length()) profiles.add(a.getString(i)) } catch (_:Exception) {}
        try { val j=JSONObject(prefs.getString("snapshots","{}")); profiles.forEach { key-> if(j.has(key)) snapshots[key]=j.getJSONObject(key) } } catch (_:Exception) {}
        try { val j=JSONObject(prefs.getString("notes","{}")); weapons.forEach { notes[it]=j.optString(it,"") } } catch (_:Exception) {}
        try { val a=JSONArray(prefs.getString("history","[]")); for(i in 0 until a.length()) { val o=a.getJSONObject(i); history.add(o.getInt("s") to o.getInt("h")) } } catch (_:Exception) {}
    }
    private fun save() {
        val s=JSONObject(); sens.forEach { (k,v)->s.put(k,v) }
        fun enc(list:List<Pair<String,Int>>) = JSONArray().also { a->list.forEach { (n,v)->a.put(JSONObject().put("n",n).put("v",v)) } }
        val p=JSONArray(); profiles.forEach { p.put(it) }; val snap=JSONObject(); snapshots.forEach { (k,v)->snap.put(k,v) }; val n=JSONObject(); notes.forEach { (k,v)->n.put(k,v) }
        val h=JSONArray(); history.forEach { (shots,heads)->h.put(JSONObject().put("s",shots).put("h",heads)) }
        prefs.edit().putString("sens",s.toString()).putString("dpi",enc(dpi).toString()).putString("fire",enc(fire).toString())
            .putString("profiles",p.toString()).putString("snapshots",snap.toString()).putString("notes",n.toString()).putString("history",h.toString())
            .putInt("sel",selectedDpi.coerceIn(0,dpi.lastIndex)).putInt("size",fireSize).apply()
    }
    private fun page(name:String) {
        body.removeAllViews()
        when(name) {
            "Home" -> {
                card { text(this,"PERFORMANCE HUB",10f,red,true); text(this,"Train with precision.",25f,-1,true,10); text(this,"Your personal setup and practice, all in one place.",13f,muted,false,7); button(this,"OPEN SENSITIVITY  →") { page("Sensitivity") } }
                heading("Quick access","Your training tools")
                listOf("Sensitivity","DPI","Fire Button","Weapons","Headshot","Profiles").forEach { dest-> card { text(this,dest,15f,-1,true); text(this,"Tap to open your " + dest.lowercase() + " tools",12f,muted,false,5); setOnClickListener { page(dest) } } }
            }
            "Sensitivity" -> {
                heading(name,"Fine tune your response values · 0–200")
                sens.keys.toList().forEach { key-> card {
                    val r=LinearLayout(this@MainActivity).apply { gravity=Gravity.CENTER_VERTICAL }
                    text(r,key,14f,-1,true); val v=text(r,sens[key].toString(),14f,red,true); addView(r)
                    val seek=SeekBar(this@MainActivity).apply { max=200; progress=sens[key]!!; progressTintList=android.content.res.ColorStateList.valueOf(red); thumbTintList=android.content.res.ColorStateList.valueOf(red) }
                    addView(seek); seek.setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener {
                        override fun onProgressChanged(s:SeekBar?,p:Int,u:Boolean) { v.text=p.toString(); sens[key]=p; save() }
                        override fun onStartTrackingTouch(s:SeekBar?) {} override fun onStopTrackingTouch(s:SeekBar?) {}
                    })
                } }
            }
            "DPI" -> {
                heading("DPI profiles","Choose, edit, and save your touch setup")
                dpi.forEachIndexed { i,item-> card {
                    text(this,item.first + "   ·   " + item.second + " DPI",15f,-1,true)
                    if(i==selectedDpi) text(this,"CURRENT PROFILE",9f,red,true,5)
                    actions(this,listOf("SELECT","EDIT","DELETE"),listOf({selectedDpi=i;save();page(name)},{pairDialog("Edit DPI profile",item.first,item.second,100,1000){n,v->dpi[i]=n to v;save();page(name)}},{if(dpi.size>1){dpi.removeAt(i);selectedDpi=0;save();page(name)}else toast("Keep at least one profile")}))
                } }
                button(body,"＋  NEW DPI PROFILE") { pairDialog("New DPI profile","My Profile",450,100,1000){n,v->dpi.add(n to v);selectedDpi=dpi.lastIndex;save();page(name)} }
            }
            "Fire Button" -> {
                heading(name,"Set a comfortable button size and save layouts")
                card { val r=LinearLayout(this@MainActivity).apply { gravity=Gravity.CENTER_VERTICAL }; text(r,"Button size",15f,-1,true); val v=text(r,fireSize.toString()+"%",15f,red,true); addView(r)
                    val seek=SeekBar(this@MainActivity).apply { max=90; progress=fireSize-10; progressTintList=android.content.res.ColorStateList.valueOf(red); thumbTintList=android.content.res.ColorStateList.valueOf(red) }; addView(seek)
                    seek.setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener { override fun onProgressChanged(s:SeekBar?,p:Int,u:Boolean){fireSize=p+10;v.text=fireSize.toString()+"%";save()}; override fun onStartTrackingTouch(s:SeekBar?){}; override fun onStopTrackingTouch(s:SeekBar?){} })
                }
                heading("Saved configurations","Apply a saved button size")
                fire.forEachIndexed { i,item->card { text(this,item.first+"   ·   "+item.second+"%",14f,-1,true); actions(this,listOf("APPLY","EDIT","DELETE"),listOf({fireSize=item.second;save();page(name)},{pairDialog("Edit configuration",item.first,item.second,10,100){n,v->fire[i]=n to v;save();page(name)}},{if(fire.size>1){fire.removeAt(i);save();page(name)}else toast("Keep at least one configuration")})) } }
                button(body,"＋  SAVE CURRENT SIZE") { inputDialog("Save configuration","Configuration "+(fire.size+1)){fire.add(it to fireSize);save();page(name)} }
            }
            "Weapons" -> {
                heading("Weapon training","Personal practice notes for every weapon")
                weapons.forEach { w->card {
                    text(this,w.uppercase(),14f,-1,true)
                    val e=EditText(this@MainActivity).apply { setText(notes[w]?: ""); hint="Add your practice notes…";setHintTextColor(Color.GRAY);setTextColor(-1);textSize=13f;minLines=2;gravity=Gravity.TOP;inputType=InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE;background=shape(bg,10);setPadding(d(10),d(9),d(10),d(9)) }
                    addView(e,LinearLayout.LayoutParams(-1,-2).apply { topMargin=d(9) }); button(this,"SAVE NOTES"){notes[w]=e.text.toString();save();toast(w+" notes saved")}
                } }
            }
            "Headshot" -> {
                val shots=history.sumOf{it.first};val heads=history.sumOf{it.second};val pct=if(shots==0)0 else heads*100/shots
                heading(name,"Record practice rounds and follow your progress")
                card { text(this,"OVERALL ACCURACY",10f,muted,true); text(this,pct.toString()+"%",42f,red,true,5); text(this,heads.toString()+" headshots  /  "+shots+" shots",13f,muted,false,4) }
                card { text(this,"LOG PRACTICE SESSION",12f,red,true); val s=number(this,"Shots attempted",100);val h=number(this,"Headshots achieved",65);button(this,"SAVE SESSION"){val a=s.text.toString().toIntOrNull();val b=h.text.toString().toIntOrNull();if(a==null||b==null||a<1||b<0||b>a)toast("Enter valid shots and headshots")else{history.add(a to b);save();page(name)}} }
                heading("Progress history",history.size.toString()+" sessions saved",18)
                if(history.isEmpty()) card { text(this,"No sessions yet. Log your first practice round above.",13f,muted,false) }
                history.indices.reversed().forEach { i->val item=history[i];card { text(this,"SESSION "+(i+1)+"    ·    "+(item.second*100/item.first)+"%",14f,red,true);text(this,item.second.toString()+" headshots from "+item.first+" shots",13f,-1,false,6);button(this,"DELETE SESSION"){history.removeAt(i);save();page(name)} } }
            }
            "Profiles" -> {
                heading(name,"Save and manage local training setups")
                card { text(this,"Save a named snapshot of your sensitivity, DPI, and fire button setup.",13f,muted,false);button(this,"＋  CREATE PROFILE"){inputDialog("Create profile","My Setup"){if(it.isNotBlank()){profiles.add(it);snapshots[it]=snapshot();save();page(name)}}} }
                profiles.forEachIndexed { i,p->val saved=snapshots[p]?:snapshot();card { text(this,p,15f,-1,true);text(this,"DPI "+saved.optInt("dpi",420)+"  ·  Fire "+saved.optInt("fire",fireSize)+"%",12f,muted,false,5);actions(this,listOf("EDIT NAME","DELETE"),listOf({inputDialog("Edit profile",p){if(it.isNotBlank()){val old=profiles[i];profiles[i]=it;snapshots[it]=snapshots.remove(old)?:saved;save();page(name)}},{AlertDialog.Builder(this@MainActivity).setTitle("Delete profile?").setMessage("Remove "+p+" from this device?").setNegativeButton("CANCEL",null).setPositiveButton("DELETE"){_,_->profiles.removeAt(i);snapshots.remove(p);save();page(name)}.show()}))} }
                if(profiles.isEmpty()) card { text(this,"No profiles saved yet.",13f,muted,false) }
            }
        }
        body.alpha=0f;body.animate().alpha(1f).translationY(0f).setDuration(200).start()
    }
    private fun heading(t:String,s:String,size:Int=24){text(body,t,size.toFloat(),-1,true,8);text(body,s,12f,muted,false,5)}
    private fun snapshot():JSONObject{val s=JSONObject();val a=JSONObject();sens.forEach{(k,v)->a.put(k,v)};s.put("sensitivity",a);s.put("dpi",dpi[selectedDpi].second);s.put("fire",fireSize);return s}
    private fun card(f:LinearLayout.()->Unit){val c=LinearLayout(this).apply{orientation=1;setPadding(d(15),d(15),d(15),d(14));background=shape(panel,16);f()};body.addView(c,LinearLayout.LayoutParams(-1,-2).apply{topMargin=d(11)})}
    private fun text(parent:LinearLayout,value:String,size:Float,color:Int,bold:Boolean,top:Int=0):TextView{val t=TextView(this).apply{text=value;textSize=size;setTextColor(color);if(bold)typeface=Typeface.DEFAULT_BOLD};parent.addView(t,LinearLayout.LayoutParams(-2,-2).apply{if(top>0)topMargin=d(top)});return t}
    private fun button(parent:LinearLayout,value:String,act:()->Unit){val b=TextView(this).apply{text=value;textSize=11f;typeface=Typeface.DEFAULT_BOLD;gravity=Gravity.CENTER;setTextColor(-1);background=shape(red,11);setPadding(d(12),d(12),d(12),d(12));setOnClickListener{animate().scaleX(.97f).scaleY(.97f).setDuration(70).withEndAction{animate().scaleX(1f).scaleY(1f).setDuration(90).start();act()}.start()}};parent.addView(b,LinearLayout.LayoutParams(-1,-2).apply{topMargin=d(10)})}
    private fun actions(parent:LinearLayout,labels:List<String>,acts:List<()->Unit>){val r=LinearLayout(this).apply{orientation=0};labels.forEachIndexed{i,l->val b=TextView(this@MainActivity).apply{text=l;textSize=9f;typeface=Typeface.DEFAULT_BOLD;gravity=Gravity.CENTER;setTextColor(if(l=="DELETE")red else -1);background=shape(Color.rgb(40,40,48),9);setPadding(d(10),d(9),d(10),d(9));setOnClickListener{acts[i]()}};r.addView(b,LinearLayout.LayoutParams(-2,-2).apply{rightMargin=d(7)})};parent.addView(r,LinearLayout.LayoutParams(-1,-2).apply{topMargin=d(9)})}
    private fun number(parent:LinearLayout,hint:String,value:Int):EditText{val e=EditText(this).apply{setText(value.toString());this.hint=hint;setTextColor(-1);setHintTextColor(muted);inputType=InputType.TYPE_CLASS_NUMBER;background=shape(bg,9);setPadding(d(10),d(7),d(10),d(7))};parent.addView(e,LinearLayout.LayoutParams(-1,d(48)).apply{topMargin=d(9)});return e}
    private fun inputDialog(title:String,value:String,done:(String)->Unit){val e=EditText(this).apply{setText(value);setTextColor(-1)};AlertDialog.Builder(this).setTitle(title).setView(e).setNegativeButton("CANCEL",null).setPositiveButton("SAVE"){_,_->done(e.text.toString().trim())}.show()}
    private fun pairDialog(title:String,name:String,value:Int,min:Int,max:Int,done:(String,Int)->Unit){val box=LinearLayout(this).apply{orientation=1;setPadding(d(20),d(5),d(20),d(5))};val n=EditText(this).apply{setText(name);setTextColor(-1);hint="Profile name"};val v=EditText(this).apply{setText(value.toString());setTextColor(-1);hint="Value";inputType=InputType.TYPE_CLASS_NUMBER};box.addView(n);box.addView(v);AlertDialog.Builder(this).setTitle(title).setView(box).setNegativeButton("CANCEL",null).setPositiveButton("SAVE"){_,_->val x=v.text.toString().toIntOrNull();if(n.text.isNullOrBlank()||x==null||x !in min..max)toast("Enter a name and value from "+min+" to "+max)else done(n.text.toString().trim(),x)}.show()}
    private fun shape(color:Int,radius:Int)=GradientDrawable().apply{setColor(color);cornerRadius=d(radius).toFloat()}
    private fun d(x:Int)=(x*resources.displayMetrics.density).toInt()
    private fun toast(msg:String)=Toast.makeText(this,msg,Toast.LENGTH_SHORT).show()
}
