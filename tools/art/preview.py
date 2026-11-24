"""Xuất ảnh xem thử: python3 preview.py chess out.png"""
import sys, os
sys.path.insert(0, os.path.dirname(__file__))
from dsl import *
import chess_pieces as cp

def shadow_svg(cx=50, cy=91, rx=30, ry=5):
    return (f'<ellipse cx="{cx}" cy="{cy}" rx="{rx}" ry="{ry}" fill="#000" fill-opacity="0.18"/>'
            f'<ellipse cx="{cx}" cy="{cy}" rx="{rx*0.72}" ry="{ry*0.7}" fill="#000" fill-opacity="0.18"/>')

def piece_svg(layers, pal, S=170, x=0, y=0, pfx='p'):
    sv = Svg(); body = sv.render(layers, pal, pfx)
    return f'<g transform="translate({x},{y}) scale({S/100})"><defs>{"".join(sv.defs)}</defs>{shadow_svg()}{body}</g>'

def chess_sheet(path):
    S = 170; W = 6 * S + 20; H = 2 * S + 20
    parts = [f'<svg xmlns="http://www.w3.org/2000/svg" width="{W}" height="{H}">']
    for row, (pal, name) in enumerate([(cp.PAL_WHITE, 'w'), (cp.PAL_BLACK, 'b')]):
        for col, t in enumerate([6, 5, 4, 3, 2, 1]):
            light = (row + col) % 2 == 0
            bg = '#F4F0E6' if (row + col) % 2 == 0 else '#17171C'
            parts.append(f'<rect x="{10+col*S}" y="{10+row*S}" width="{S}" height="{S}" fill="{bg}"/>')
            parts.append(piece_svg(cp.PIECES[t][1](), pal, S * 0.92, 10 + col * S + S * 0.04, 10 + row * S + S * 0.02, f'{name}{t}'))
    parts.append('</svg>')
    return ''.join(parts)

def render(svg, out, w, h, scale=2):
    from playwright.sync_api import sync_playwright
    html = f'<html><body style="margin:0;background:#ddd">{svg}</body></html>'
    with sync_playwright() as p:
        b = p.chromium.launch()
        pg = b.new_page(viewport={'width': w, 'height': h}, device_scale_factor=scale)
        pg.set_content(html); pg.screenshot(path=out); b.close()

if __name__ == '__main__':
    what, out = sys.argv[1], sys.argv[2]
    if what == 'chess':
        svg = chess_sheet(out); render(svg, out, 6 * 170 + 20, 2 * 170 + 20)


def other_sheet():
    import other_pieces as op
    S = 150
    parts = [f'<svg xmlns="http://www.w3.org/2000/svg" width="{6*S+20}" height="{2*S+20}">']
    parts.append('<defs><linearGradient id="gold" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#F3D98B"/><stop offset=".5" stop-color="#D9AE4A"/><stop offset="1" stop-color="#EFCF7A"/></linearGradient>'
                 '<linearGradient id="oak" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#F3E8D3"/><stop offset="1" stop-color="#E6D5B6"/></linearGradient></defs>')
    parts.append(f'<rect x="10" y="10" width="{3*S}" height="{2*S}" fill="url(#gold)"/>')
    parts.append(f'<rect x="{10+3*S}" y="10" width="{3*S}" height="{2*S}" fill="url(#oak)"/>')
    for k in range(0, 3 * S + 1, S):
        parts.append(f'<line x1="{10+k}" y1="10" x2="{10+k}" y2="{10+2*S}" stroke="#2a1b05" stroke-width="1.2"/>')
        parts.append(f'<line x1="{10+3*S+k}" y1="10" x2="{10+3*S+k}" y2="{10+2*S}" stroke="#4a3a28" stroke-opacity=".6" stroke-width="1"/>')
    for k in range(0, 2 * S + 1, S):
        parts.append(f'<line x1="10" y1="{10+k}" x2="{10+3*S}" y2="{10+k}" stroke="#2a1b05" stroke-width="1.2"/>')
        parts.append(f'<line x1="{10+3*S}" y1="{10+k}" x2="{10+6*S}" y2="{10+k}" stroke="#4a3a28" stroke-opacity=".6" stroke-width="1"/>')
    chars = [('帥', True), ('將', False), ('車', True), ('馬', False), ('炮', True), ('卒', False)]
    for i, (ch, red) in enumerate(chars):
        x, y = 10 + (i % 3) * S, 10 + (i // 3) * S
        ly = op.XQ_RED if red else op.XQ_BLACK
        sv = Svg(); b = sv.render(ly, {}, f'x{i}')
        parts.append(f'<g transform="translate({x},{y}) scale({S/100})"><defs>{"".join(sv.defs)}</defs>'
                     f'<ellipse cx="52" cy="86" rx="38" ry="9" fill="#000" fill-opacity=".22"/>{b}'
                     f'<text x="50" y="66" font-size="44" text-anchor="middle" font-weight="700" font-family="serif" fill="{"#B3122B" if red else "#0C0C12"}">{ch}</text></g>')
    for i, kind in enumerate(['x', 'o', 'x', 'o', 'o', 'x']):
        x, y = 10 + 3 * S + (i % 3) * S, 10 + (i // 3) * S
        ly = op.caro_x() if kind == 'x' else op.caro_o()
        sv = Svg(); b = sv.render(ly, {}, f'c{i}')
        parts.append(f'<g transform="translate({x},{y}) scale({S/100})"><defs>{"".join(sv.defs)}</defs>'
                     f'<ellipse cx="53" cy="62" rx="30" ry="22" fill="#000" fill-opacity=".14"/>{b}</g>')
    parts.append('</svg>')
    return ''.join(parts)

if __name__ == '__main__' and sys.argv[1] == 'other':
    render(other_sheet(), sys.argv[2], 6 * 150 + 20, 2 * 150 + 20)
