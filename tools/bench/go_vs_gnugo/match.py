import subprocess, sys, time
GN="/usr/games/gnugo"; JAR="/home/claude/gogtp.jar:/home/claude/kotlinc_dist/kotlinc/lib/kotlin-stdlib.jar"
class Eng:
    def __init__(self, cmd): self.p=subprocess.Popen(cmd,stdin=subprocess.PIPE,stdout=subprocess.PIPE,stderr=subprocess.DEVNULL,text=True,bufsize=1)
    def send(self, c):
        self.p.stdin.write(c+"\n"); self.p.stdin.flush()
        out=[]
        while True:
            l=self.p.stdout.readline()
            if l is None or l=="": break
            if l.strip()=="" and out: break
            if l.strip()!="": out.append(l.strip())
        r=" ".join(out)
        return r[1:].strip().split(" ",1)[-1] if r[:1]=="=" and " " in r else ("" if r[:1]=="=" else r)
    def quit(self):
        try: self.p.stdin.write("quit\n"); self.p.stdin.flush()
        except: pass
        self.p.terminate()
def area_score(gn):
    """Chấm điểm diện tích sau khi gỡ quân chết theo GNU Go (final_status_list dead)."""
    gn.p.stdin.write("showboard\n"); gn.p.stdin.flush(); lines=[]
    while True:
        l=gn.p.stdout.readline()
        if l=="": break
        if l.strip()=="" and lines: break
        if l.strip(): lines.append(l.rstrip("\n"))
    rows=[x for x in lines if x.strip() and x.strip()[0].isdigit()]
    grid={}
    for ln in rows:
        parts=ln.split(); r=int(parts[0]); cells=parts[1:10]
        for c,ch in enumerate(cells): grid[(r,c)]=ch
    dead=gn.send("final_status_list dead")
    deadcells=set()
    for tok in dead.replace("=","").split():
        tok=tok.strip().upper()
        if len(tok)>=2 and tok[0].isalpha() and tok[1:].isdigit():
            col=ord(tok[0])-65-(1 if tok[0]>"I" else 0); deadcells.add((int(tok[1:]),col))
    g={k:("." if (k in deadcells) else v) for k,v in grid.items()}
    b=w=0; seen=set()
    for k,v in g.items():
        if v=="X": b+=1
        elif v=="O": w+=1
        elif v=="." and k not in seen:
            stack=[k]; seen.add(k); size=0; tb=tw=False
            while stack:
                r,c=stack.pop(); size+=1
                for dr,dc in((1,0),(-1,0),(0,1),(0,-1)):
                    q=(r+dr,c+dc)
                    if q not in g: continue
                    if g[q]=="X": tb=True
                    elif g[q]=="O": tw=True
                    elif q not in seen: seen.add(q); stack.append(q)
            if tb and not tw: b+=size
            elif tw and not tb: w+=size
    d=b-w-7.5
    return ("B+%.1f"%d) if d>0 else ("W+%.1f"%(-d))

def game(ours_black, level, ms):
    me=Eng(["java","-cp",JAR,"GoGtpKt",str(ms)]); gn=Eng([GN,"--mode","gtp","--level",str(level),"--komi","7.5","--boardsize","9"])
    for e in (me,gn): e.send("boardsize 9"); e.send("clear_board"); e.send("komi 7.5")
    turn="b"; passes=0; n=0
    while passes<2 and n<160:
        e,o=(me,gn) if (turn=="b")==ours_black else (gn,me)
        mv=e.send(f"genmove {turn}")
        mv=mv.strip().upper() if mv else "PASS"
        o.send(f"play {turn} {mv}")
        passes = passes+1 if mv=="PASS" else 0
        turn="w" if turn=="b" else "b"; n+=1
    sc=area_score(gn)
    me.quit(); gn.quit()
    return sc, n
if __name__=="__main__":
    level=int(sys.argv[1]); ms=int(sys.argv[2]); games=int(sys.argv[3]); w=0
    for i in range(games):
        ob = (i%2==0)
        sc,n=game(ob,level,ms)
        win = (sc.startswith("B") and ob) or (sc.startswith("W") and not ob)
        w+=win; print(f"game {i} ours={'B' if ob else 'W'} result={sc} moves={n} -> {'WIN' if win else 'loss'}", flush=True)
    print(f"RESULT level {level} {ms}ms: {w}/{games}")
