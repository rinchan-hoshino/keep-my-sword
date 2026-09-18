"""Simplify the vanilla diamond sword palette; preserve every source pixel position."""
from PIL import Image
from pathlib import Path
out=Path(__file__).parent
src=Image.open(out/'vanilla-diamond-sword.png').convert('RGBA')
# Eleven opaque source colors become seven. Cyan identifies diamond;
# the former green shadow and outline receive the damage tint.
palette={
 (164,253,240):(177,250,234),
 (51,235,203):(77,218,200),
 (43,199,172):(39,166,151),
 (30,138,119):(206,104,109),
 (21,99,85):(206,104,109),
 (14,63,54):(99,50,57),
 (8,37,32):(99,50,57),
 (137,103,39):(157,116,61),
 (104,78,30):(157,116,61),
 (73,54,21):(89,65,37),
 (40,30,11):(89,65,37),
}
im=Image.new('RGBA',src.size)
im.putdata([(*palette[p[:3]],255) if p[3] else (0,0,0,0) for p in src.getdata()])
assert list(im.getchannel('A').getdata())==list(src.getchannel('A').getdata())
im.save(out/'simplified-16.png')
im.resize((48,48),Image.Resampling.NEAREST).save(out/'icon-48.png')
im.resize((576,576),Image.Resampling.NEAREST).save(out/'preview.png')
platform=Image.new('RGBA',(400,400));platform.paste(im.resize((384,384),Image.Resampling.NEAREST),(8,8));platform.save(out/'icon.png')
