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
    private val tabs = listOf("Home","Setup","Play","Sensitivity","DPI","Fire Button","Weapons","Headshot","Profiles","About")
    private val weapons = listOf("Desert Eagle","Woodpecker","SVD","M1887","UMP","MP40","M4A1","SCAR","AK","AWM","Other")
    private val sens = linkedMapOf("General" to 100,"Red Dot" to 95,"2X Scope" to 90,"4X Scope" to 85,"Sniper Scope" to 70,"Free Camera" to 100)
    private val dpi = mutableListOf<Pair<String,Int>>()
    private val fire = mutableListOf<Pair<String,Int>>()
    private val profiles = mutableListOf<String>()
    private val snapshots = mutableMapOf<String, JSONObject>()
    private val notes = mutableMapOf<String,String>()
    private val history = mutableListOf<Pair<Int,Int>>()
    private var selectedDpi = 0; private var fireSize = 48
    private var brand = "Other"; private var model = android.os.Build.MODEL; private var refreshRate = 60; private var preferredFps = 60; private var fingers = "3-finger"; private var aimFeedback = "Performing well"; private var selectedProfile = "Balanced"

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme); super.onCreate(savedInstanceState); window.statusBarColor=bg; window.navigationBarColor=bg
        prefs=getSharedPreferences("rauf_pnl",MODE_PRIVATE); load()
        val root=LinearLayout(this).apply { orientation=1; setBackgroundColor(bg) }
        val head=LinearLayout(this).apply { orientation=0; gravity=Gravity.CENTER_VERTICAL; setPadding(d(18),d(14),d(18),d(10)) }
        val logo=TextView(this).apply { text="RB"; textSize=17f; typeface=Typeface.DEFAULT_BOLD; gravity=Gravity.CENTER; setTextColor(-1); background=shape(red,14); }
        head.addView(logo,LinearLayout.LayoutParams(d(44),d(44)))
        val title=LinearLayout(this).apply { orientation=1; setPadding(d(12),0,0,0) }
        text(title,"RAUF BRANDS",18f,-1,true); text(title,"TRAINING COMPANION",9f,red,true); head.addView(title,LinearLayout.LayoutParams(0,-2,1f))
        text(head,"● LOCAL",10f,red,true); root.addView(head)
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
        brand=prefs.getString("brand","Other")?:"Other"; model=prefs.getString("model",android.os.Build.MODEL)?:android.os.Build.MODEL; refreshRate=prefs.getInt("refresh",60); preferredFps=prefs.getInt("fps",60); fingers=prefs.getString("fingers","3-finger")?:"3-finger"; aimFeedback=prefs.getString("aim","Performing well")?:"Performing well"; selectedProfile=prefs.getString("generator_profile","Balanced")?:"Balanced"
        selectedDpi=prefs.getInt("sel",0).coerceIn(0,dpi.lastIndex); fireSize=prefs.getInt("size",48).coerceIn(30,100)
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
            .putInt("sel",selectedDpi.coerceIn(0,dpi.lastIndex)).putInt("size",fireSize).putString("brand",brand).putString("model",model).putInt("refresh",refreshRate).putInt("fps",preferredFps).putString("fingers",fingers).putString("aim",aimFeedback).putString("generator_profile",selectedProfile).apply()
    }
    private fun page(name:String) {
        body.removeAllViews()
        when(name) {
            "Home" -> {
                card { text(this,"RAUF BRANDS · PERFORMANCE HUB",10f,red,true); text(this,"Train with precision.",25f,-1,true,10); text(this,"Your personal setup and practice, all in one place. Settings are saved on this device and are not applied to Free Fire MAX.",13f,muted,false,7); button(this,"OPEN SENSITIVITY  →") { page("Sensitivity") } }
                heading("Quick access","Your training tools")
                listOf("Setup","Play","Sensitivity","DPI","Fire Button","Weapons","Headshot","Profiles").forEach { dest-> card { text(this,dest,15f,-1,true); text(this,"Tap to open your " + dest.lowercase() + " tools",12f,muted,false,5); setOnClickListener { page(dest) } } }
            }
            "Play" -> {
                heading("Arena practice","Offline touch aim trainer · frame-synced rendering")
                card { text(this,"A standalone target practice mini-game with local scoring and adjustable touch target size.",13f,muted,false); text(this,"It does not connect to Free Fire MAX, provide aim assistance, or use a game server.",12f,muted,false,7); button(this,"START PRACTICE  →") { startActivity(android.content.Intent(this@MainActivity,ArenaActivity::class.java)) } }
            }
            "Setup" -> {
                val screenWidth = resources.configuration.smallestScreenWidthDp.coerceAtLeast(320)
                val deviceClass = when {
                    screenWidth < 360 -> "Compact phone"
                    screenWidth < 420 -> "Standard phone"
                    screenWidth < 600 -> "Large phone"
                    else -> "Tablet / large display"
                }
                heading("Smart setup", "A practical starting point for your device")
                card {
                    text(this,"YOUR DEVICE",10f,red,true)
                    text(this,"$brand $model",18f,-1,true,6)
                    val brands=listOf("OnePlus","Samsung","Xiaomi","Redmi","Realme","Vivo","Oppo","Tecno","Infinix","Other")
                    val brandPick=Spinner(this@MainActivity).apply { adapter=ArrayAdapter(this@MainActivity,android.R.layout.simple_spinner_dropdown_item,brands); setSelection(brands.indexOf(brand).coerceAtLeast(0)); onItemSelectedListener=object:android.widget.AdapterView.OnItemSelectedListener { override fun onNothingSelected(p:android.widget.AdapterView<*>?){}; override fun onItemSelected(p:android.widget.AdapterView<*>?,v:android.view.View?,pos:Int,id:Long){brand=brands[pos];save()} } }
                    addView(brandPick,LinearLayout.LayoutParams(-1,d(48)))
                    val modelField=EditText(this@MainActivity).apply { setText(model); hint="Phone model"; setTextColor(-1); setHintTextColor(muted); background=shape(bg,9);setPadding(d(10),d(6),d(10),d(6)) }
                    addView(modelField,LinearLayout.LayoutParams(-1,d(48)).apply{topMargin=d(7)})
                    button(this,"SAVE DEVICE DETAILS"){model=modelField.text.toString().ifBlank{android.os.Build.MODEL};save();toast("Device details saved")}
                    text(this,"Smallest width: $screenWidth dp · Android display density: ${resources.displayMetrics.densityDpi} dpi",12f,muted,false,5)
                    text(this,"Android ${android.os.Build.VERSION.RELEASE} · ${windowManager.defaultDisplay.refreshRate.toInt()} Hz display",12f,muted,false,5)
                    val hz=number(this,"Refresh rate (Hz)",refreshRate); val fps=number(this,"Preferred FPS",preferredFps)
                    val fingerPick=Spinner(this@MainActivity).apply { val choices=listOf("2-finger","3-finger","4-finger");adapter=ArrayAdapter(this@MainActivity,android.R.layout.simple_spinner_dropdown_item,choices);setSelection(choices.indexOf(fingers).coerceAtLeast(0));onItemSelectedListener=object:android.widget.AdapterView.OnItemSelectedListener{override fun onNothingSelected(p:android.widget.AdapterView<*>?){};override fun onItemSelected(p:android.widget.AdapterView<*>?,v:android.view.View?,pos:Int,id:Long){fingers=choices[pos];save()}} }
                    addView(fingerPick,LinearLayout.LayoutParams(-1,d(48)).apply{topMargin=d(7)})
                    button(this,"SAVE DEVICE PREFERENCES"){refreshRate=(hz.text.toString().toIntOrNull()?:60).coerceIn(30,240);preferredFps=(fps.text.toString().toIntOrNull()?:60).coerceIn(30,240);save();toast("Preferences saved")}
                    text(this,"These are editable in-game starting values, not a universal best setting. Fine-tune them in Training Mode.",12f,muted,false,8)
                }
                card {
                    text(this,"PICK YOUR PLAY STYLE",10f,red,true)
                    val styles = listOf("Headshot","One Tap","Drag Headshot","Balanced","Close Range","Long Range")
                    val style = Spinner(this@MainActivity).apply {
                        adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, styles)
                    }
                    addView(style, LinearLayout.LayoutParams(-1,d(48)).apply { topMargin=d(6) })
                    val results = LinearLayout(this@MainActivity).apply { orientation=1 }
                    addView(results, LinearLayout.LayoutParams(-1,-2).apply { topMargin=d(8) })
                    fun showRecommendation(selectedStyle: String) {
                        results.removeAllViews()
                        val scale = when {
                            screenWidth < 360 -> 0.92
                            screenWidth >= 600 -> 1.06
                            else -> 1.0
                        }
                        selectedProfile=selectedStyle
                        val styleOffset = when(selectedStyle) { "Headshot" -> 14; "One Tap" -> 18; "Drag Headshot" -> 10; "Close Range" -> 12; "Long Range" -> -10; else -> 0 }
                        val recommendedDpi = (420 * scale).toInt().coerceIn(300,650)
                        val deviceOffset=when { refreshRate>=120 -> 4; refreshRate<=60 -> -3; else -> 0 }
                        val aimOffset=when(aimFeedback){"Aim goes above the head" -> -6; "Aim sticks to the body" -> 6; else -> 0}
                        val base = (150 * scale).toInt() + styleOffset + deviceOffset + aimOffset
                        val recommended = linkedMapOf(
                            "General" to base.coerceIn(0,200), "Red Dot" to (base-5).coerceIn(0,200),
                            "2X Scope" to (base-12).coerceIn(0,200), "4X Scope" to (base-22).coerceIn(0,200),
                            "Sniper Scope" to (base-45).coerceIn(0,200), "Free Camera" to (base-10).coerceIn(0,200)
                        )
                        text(results,"RECOMMENDED STARTING VALUES",10f,red,true)
                        text(results,"DPI  $recommendedDpi",17f,-1,true,7)
                        val feedback=Spinner(this@MainActivity).apply { val options=listOf("Performing well","Aim goes above the head","Aim sticks to the body");adapter=ArrayAdapter(this@MainActivity,android.R.layout.simple_spinner_dropdown_item,options);setSelection(options.indexOf(aimFeedback).coerceAtLeast(0));onItemSelectedListener=object:android.widget.AdapterView.OnItemSelectedListener{override fun onNothingSelected(p:android.widget.AdapterView<*>?){};override fun onItemSelected(p:android.widget.AdapterView<*>?,v:android.view.View?,pos:Int,id:Long){aimFeedback=options[pos];save();showRecommendation(styles[style.selectedItemPosition.coerceIn(0,styles.lastIndex)])}} }
                        text(results,"AIM FEEDBACK",10f,red,true,9);results.addView(feedback,LinearLayout.LayoutParams(-1,d(48)))
                        recommended.forEach { (label,value)->
                            val row=LinearLayout(this@MainActivity).apply { gravity=Gravity.CENTER_VERTICAL; setPadding(0,d(5),0,d(5)) }
                            text(row,label,13f,muted,false)
                            val valueView=text(row,value.toString(),14f,-1,true)
                            (row.getChildAt(0).layoutParams as LinearLayout.LayoutParams).weight=1f
                            (row.getChildAt(0).layoutParams as LinearLayout.LayoutParams).width=0
                            valueView.gravity=Gravity.END
                            results.addView(row)
                        }
                        button(results,"USE THESE VALUES") {
                            recommended.forEach { (key,value)->sens[key]=value }
                            val existing=dpi.indexOfFirst{it.first=="Smart setup"}
                            if(existing>=0) dpi[existing]="Smart setup" to recommendedDpi else dpi.add("Smart setup" to recommendedDpi)
                            selectedDpi=dpi.indexOfFirst{it.first=="Smart setup"}
                            save(); toast("Setup saved. Enter the values in Free Fire MAX settings.")
                        }
                    }
                    style.onItemSelectedListener=object:android.widget.AdapterView.OnItemSelectedListener {
                        override fun onNothingSelected(parent:android.widget.AdapterView<*>?) {}
                        override fun onItemSelected(parent:android.widget.AdapterView<*>?,view:android.view.View?,position:Int,id:Long) { showRecommendation(styles[position]) }
                    }
                }
            }
            "Sensitivity" -> {
                heading(name,"Fine tune your response values · 0–200")
                card {
                    text(this,"GENERATE A STARTING PROFILE",10f,red,true)
                    val presets=listOf("Headshot","One Tap","Drag Headshot","Balanced","Close Range","Long Range")
                    val profile=Spinner(this@MainActivity).apply { adapter=ArrayAdapter(this@MainActivity,android.R.layout.simple_spinner_dropdown_item,presets);setSelection(presets.indexOf(selectedProfile).coerceAtLeast(0)) }
                    addView(profile,LinearLayout.LayoutParams(-1,d(48)).apply{topMargin=d(6)})
                    val feedbackOptions=listOf("Performing well","Aim goes above the head","Aim sticks to the body")
                    val feedback=Spinner(this@MainActivity).apply { adapter=ArrayAdapter(this@MainActivity,android.R.layout.simple_spinner_dropdown_item,feedbackOptions);setSelection(feedbackOptions.indexOf(aimFeedback).coerceAtLeast(0)) }
                    addView(feedback,LinearLayout.LayoutParams(-1,d(48)).apply{topMargin=d(4)})
                    button(this,"GENERATE SETTINGS") {
                        selectedProfile=presets[profile.selectedItemPosition];aimFeedback=feedbackOptions[feedback.selectedItemPosition]
                        val offset=when(selectedProfile){"Headshot"->14;"One Tap"->18;"Drag Headshot"->10;"Close Range"->12;"Long Range"->-10;else->0}
                        val deviceOffset=when{refreshRate>=120->4;refreshRate<=60->-3;else->0};val aimOffset=when(aimFeedback){"Aim goes above the head"->-6;"Aim sticks to the body"->6;else->0}
                        val base=(150+offset+deviceOffset+aimOffset).coerceIn(0,200)
                        val generated=linkedMapOf("General" to base,"Red Dot" to (base-5).coerceIn(0,200),"2X Scope" to (base-12).coerceIn(0,200),"4X Scope" to (base-22).coerceIn(0,200),"Sniper Scope" to (base-45).coerceIn(0,200),"Free Camera" to (base-10).coerceIn(0,200))
                        generated.forEach{(key,value)->sens[key]=value};save();page(name);toast("$selectedProfile starting values generated")
                    }
                    text(this,"Recommendations are practice starting points. Adjust them in Training Mode; results vary by player and device.",12f,muted,false,7)
                }
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
                card { text(this,"DEVICE DISPLAY",10f,red,true); text(this,"$brand $model · Android ${android.os.Build.VERSION.RELEASE}",15f,-1,true,6); text(this,"Suggested starting range: 380–480 DPI · current saved profile: ${dpi[selectedDpi].second} DPI",12f,muted,false,5); text(this,"To change Android display size: open Settings → System → Developer options → Smallest width, record the current value first, then change it gradually. This app never changes system settings. A wrong value can make the interface difficult to use; restore the recorded value if needed.",12f,muted,false,5) }
                dpi.forEachIndexed { i,item-> card {
                    text(this,item.first + "   ·   " + item.second + " DPI",15f,-1,true)
                    if(i==selectedDpi) text(this,"CURRENT PROFILE",9f,red,true,5)
                    actions(this,listOf("SELECT","EDIT","DELETE"),listOf({selectedDpi=i;save();page(name)},{pairDialog("Edit DPI profile",item.first,item.second,1,2000){n,v->dpi[i]=n to v;save();page(name)}},{if(dpi.size>1){dpi.removeAt(i);selectedDpi=0;save();page(name)}else toast("Keep at least one profile")}))
                } }
                button(body,"＋  NEW DPI PROFILE") { pairDialog("New DPI profile","My Profile",450,1,2000){n,v->dpi.add(n to v);selectedDpi=dpi.lastIndex;save();page(name)} }
            }
            "Fire Button" -> {
                heading(name,"Set a comfortable button size and save layouts")
                card { val suggested=when(fingers){"2-finger"->58;"4-finger"->44;else->50};text(this,"$fingers layout · suggested starting size $suggested%",14f,-1,true);text(this,"Choose a value from 30% to 100%; the preview scales as you adjust it.",12f,muted,false,5) }
                card { val r=LinearLayout(this@MainActivity).apply { gravity=Gravity.CENTER_VERTICAL }; text(r,"Button size",15f,-1,true); val v=text(r,fireSize.toString()+"%",15f,red,true); addView(r)
                    val preview=FrameLayout(this@MainActivity).apply { background=shape(bg,12);minimumHeight=d(155) }; val target=TextView(this@MainActivity).apply { text="FIRE";gravity=Gravity.CENTER;textSize=11f;typeface=Typeface.DEFAULT_BOLD;setTextColor(-1);background=shape(red,100) }; preview.addView(target,FrameLayout.LayoutParams(d(fireSize+30),d(fireSize+30),Gravity.CENTER)); addView(preview,LinearLayout.LayoutParams(-1,d(155)).apply{topMargin=d(12)})
                    val seek=SeekBar(this@MainActivity).apply { max=70; progress=fireSize-30; progressTintList=android.content.res.ColorStateList.valueOf(red); thumbTintList=android.content.res.ColorStateList.valueOf(red) }; addView(seek)
                    seek.setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener { override fun onProgressChanged(s:SeekBar?,p:Int,u:Boolean){fireSize=p+30;v.text=fireSize.toString()+"%";val lp=target.layoutParams as FrameLayout.LayoutParams;lp.width=d(fireSize+30);lp.height=d(fireSize+30);target.layoutParams=lp;save()}; override fun onStartTrackingTouch(s:SeekBar?){}; override fun onStopTrackingTouch(s:SeekBar?){} })
                }
                card { text(this,"POSITION IDEAS",10f,red,true); text(this,"Keep the fire control within comfortable thumb reach. Leave space around nearby controls, then adjust the layout in the game’s own HUD editor.",13f,muted,false,6) }
                heading("Saved configurations","Apply a saved button size")
                fire.forEachIndexed { i,item->card { text(this,item.first+"   ·   "+item.second+"%",14f,-1,true); actions(this,listOf("APPLY","EDIT","DELETE"),listOf({fireSize=item.second;save();page(name)},{pairDialog("Edit configuration",item.first,item.second,1,100){n,v->fire[i]=n to v;save();page(name)}},{if(fire.size>1){fire.removeAt(i);save();page(name)}else toast("Keep at least one configuration")})) } }
                button(body,"＋  SAVE CURRENT SIZE") { pairDialog("Save fire button profile","Configuration "+(fire.size+1),fireSize,30,100){n,v->fire.add(n to v);save();page(name)} }
            }
            "Weapons" -> {
                heading("Weapon profiles","Starting sensitivity notes and personal practice notes")
                weapons.forEach { w->card {
                    text(this,w.uppercase(),14f,-1,true)
                    val offset=when(w){"Desert Eagle","Woodpecker","SVD","AWM"->-8;"M1887","MP40","UMP"->8;else->0}
                    text(this,"Suggested General ${ (sens["General"]!!+offset).coerceIn(0,200)} · Red Dot ${(sens["Red Dot"]!!+offset).coerceIn(0,200)} · 2X ${(sens["2X Scope"]!!+offset/2).coerceIn(0,200)}",12f,red,true,5)
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
                card { text(this,"Save a named snapshot of your sensitivity, DPI, and fire button setup. Profiles stay on this device.",13f,muted,false);button(this,"＋  CREATE PROFILE"){inputDialog("Create profile","My Setup"){if(it.isNotBlank()&&it !in profiles){profiles.add(it);snapshots[it]=snapshot();save();page(name)}else toast("Choose a unique profile name")}} }
                if(profiles.size>=2) button(body,"COMPARE TWO PROFILES") { val choices=profiles.toTypedArray();AlertDialog.Builder(this).setTitle("Choose first profile").setItems(choices){_,a->val remaining=choices.filterIndexed{idx,_->idx!=a}.toTypedArray();AlertDialog.Builder(this).setTitle("Choose second profile").setItems(remaining){_,b->compareProfiles(choices[a],remaining[b])}.show()}.show() }
                profiles.forEachIndexed { i,p->val saved=snapshots[p]?:snapshot();card { text(this,p,15f,-1,true);text(this,"DPI "+saved.optInt("dpi",420)+"  ·  Fire "+saved.optInt("fire",fireSize)+"%",12f,muted,false,5);actions(this,listOf("LOAD","UPDATE","RENAME","DELETE"),listOf({applySnapshot(saved);page(name)},{snapshots[p]=snapshot();save();toast("Profile updated")},{inputDialog("Rename profile",p){if(it.isNotBlank()&&it !in profiles){val old=profiles[i];profiles[i]=it;snapshots[it]=snapshots.remove(old)?:saved;save();page(name)}else toast("Choose a unique name") }},{AlertDialog.Builder(this@MainActivity).setTitle("Delete profile?").setMessage("Remove "+p+" from this device?").setNegativeButton("CANCEL",null).setPositiveButton("DELETE"){_,_->profiles.removeAt(i);snapshots.remove(p);save();page(name)}.show()}))} }
                if(profiles.isEmpty()) card { text(this,"No profiles saved yet.",13f,muted,false) }
            }
            "About" -> { heading("About RAUF BRANDS","A local practice companion for Free Fire players"); card { text(this,"RAUF BRANDS",18f,red,true);text(this,"Personalize sensitivity starting points, keep device-specific setup profiles, and practice touch accuracy offline. Recommendations are starting points only; there is no guaranteed headshot setting.",13f,muted,false,8);text(this,"The app does not modify game files, inject code, automate gameplay, require root, or change Android developer settings. Profile data stays on this device.",13f,muted,false,8);text(this,"Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}\nAndroid ${android.os.Build.VERSION.RELEASE}",12f,-1,false,8) } }
        }
        body.alpha=0f;body.animate().alpha(1f).translationY(0f).setDuration(200).start()
    }
    private fun heading(t:String,s:String,size:Int=24){text(body,t,size.toFloat(),-1,true,8);text(body,s,12f,muted,false,5)}
    private fun snapshot():JSONObject{val s=JSONObject();val a=JSONObject();sens.forEach{(k,v)->a.put(k,v)};s.put("sensitivity",a);s.put("dpi",dpi[selectedDpi].second);s.put("fire",fireSize);s.put("brand",brand);s.put("model",model);s.put("fingers",fingers);s.put("weapon",prefs.getString("weapon","Desert Eagle"));return s}
    private fun applySnapshot(s:JSONObject){val values=s.optJSONObject("sensitivity");if(values!=null)sens.keys.toList().forEach{key->sens[key]=values.optInt(key,sens[key]!!).coerceIn(0,200)};val savedDpi=s.optInt("dpi",dpi[selectedDpi].second);selectedDpi=dpi.indexOfFirst{it.second==savedDpi}.takeIf{it>=0}?:selectedDpi;fireSize=s.optInt("fire",fireSize).coerceIn(30,100);brand=s.optString("brand",brand);model=s.optString("model",model);fingers=s.optString("fingers",fingers);save();toast("Profile loaded into companion")}
    private fun compareProfiles(first:String,second:String){val a=snapshots[first]?:return;val b=snapshots[second]?:return;val sa=a.optJSONObject("sensitivity")?:JSONObject();val sb=b.optJSONObject("sensitivity")?:JSONObject();val lines=mutableListOf("DPI: ${a.optInt("dpi")}  |  ${b.optInt("dpi")}","Fire size: ${a.optInt("fire")}%  |  ${b.optInt("fire")}%");sens.keys.forEach{key->lines.add("$key: ${sa.optInt(key)}  |  ${sb.optInt(key)}")};AlertDialog.Builder(this).setTitle("$first  vs  $second").setMessage(lines.joinToString("\n")).setPositiveButton("DONE",null).show()}
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
