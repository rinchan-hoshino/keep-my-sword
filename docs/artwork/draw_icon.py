"""Keep My Sword icon: a diamond sword broken in two, bound with a bandage.
Drawn on a native 32x32 grid. Run: python3 draw_icon.py  (writes icon-32.png and icon.png next to this file)"""
import math,struct,zlib
from pathlib import Path
N=32
C={'.':None,'o':(24,28,38),'b':(92,219,213),'B':(48,160,170),'h':(200,250,245),
   'g':(120,90,50),'G':(80,58,32),'y':(214,170,60),'w':(236,226,206),'W':(176,156,128),'r':(232,128,40)}
grid=[['.']*N for _ in range(N)]
def put(x,y,c):
    if 0<=x<N and 0<=y<N: grid[y][x]=c
# axis: from handle (5,26) to tip (27,4); u along, v across
ax,ay=5.0,26.0; dx,dy=1/math.sqrt(2),-1/math.sqrt(2)
def uv(x,y):
    px,py=x+0.5-ax,y+0.5-ay
    return px*dx+py*dy, -px*dy+py*dx
for y in range(N):
    for x in range(N):
        u,v=uv(x,y)
        # tip half shifted across by 0.9 (misaligned break)
        if u>19.5: v2=v-1.4
        else: v2=v
        if 0<=u<6 and abs(v)<1.1: put(x,y,'g' if (int(u*1.4))%2 else 'G')       # handle wrap
        elif -1.2<=u<0 and abs(v)<1.6: put(x,y,'y')                              # pommel
        elif 6<=u<7.6 and abs(v)<4.0: put(x,y,'y')                               # guard
        elif 7.6<=u<31 and abs(v2)<(1.7 if u<28.5 else 1.7*(31-u)/2.5):
            if 17.8<=u<=21.2: continue                                           # gap -> bandage later
            put(x,y,'h' if v2<-0.6 else ('B' if v2>0.6 else 'b'))
# bandage over the break
for y in range(N):
    for x in range(N):
        u,v=uv(x,y)
        if 17.2<=u<=21.8 and abs(v-0.7)<2.9:
            put(x,y,'W' if int((u-17.2)*1.2)%2 else 'w')
# little orange tie ends
for (x,y) in [(18,16),(18,17),(19,18),(18,19),(20,17),(21,18)]:
    put(x,y,'r')
for (u,v) in [(22.6,0.9),(23.4,0.3),(24.1,1.0)]:
    x=ax+u*dx-v*dy; y=ay+u*dy+v*dx; put(int(x),int(y),'o')
# tip half sits one cell up-left so the two pieces line up across the break
moved=[(x,y,grid[y][x]) for y in range(N) for x in range(N) if grid[y][x]!='.' and uv(x,y)[0]>21.8]
for x,y,c in moved: grid[y][x]='.'
for x,y,c in moved:
    if 0<=x-1 and 0<=y-1 and grid[y-1][x-1] not in 'wW': put(x-1,y-1,c)   # bandage stays on top
# outline
out=[r[:] for r in grid]
for y in range(N):
    for x in range(N):
        if grid[y][x]=='.':
            if any(0<=x+a<N and 0<=y+b<N and grid[y+b][x+a]!='.' for a,b in((1,0),(-1,0),(0,1),(0,-1))):
                out[y][x]='o'

def png(path,scale):
    rows=[]
    for y in range(N):
        row=b''.join((bytes(C[c])+b'\xff' if C[c] else b'\0\0\0\0')*scale for c in out[y])
        rows+= [b'\0'+row]*scale
    w=N*scale
    def chunk(t,d): return struct.pack('>I',len(d))+t+d+struct.pack('>I',zlib.crc32(t+d))
    Path(path).write_bytes(b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('>IIBBBBB',w,w,8,6,0,0,0))
        +chunk(b'IDAT',zlib.compress(b''.join(rows),9))+chunk(b'IEND',b''))
here=Path(__file__).resolve().parent
png(here/'icon-32.png',1)
png(here/'icon.png',16)
