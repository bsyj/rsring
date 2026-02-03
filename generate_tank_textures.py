from PIL import Image, ImageDraw
import math

# 设置纹理尺寸
TEXTURE_SIZE = 32

# 定义等级对应的颜色和材质
TANKS = [
    {
        "name": "100",
        "color": (160, 160, 160),  # 铁色
        "accent_color": (100, 100, 100),  # 铁色暗部
        "liquid_color": (140, 140, 140),  # 铁色液体
        "liquid_highlight": (180, 180, 180),  # 液体高光
        "gold_color": (200, 180, 100),  # 金色装饰
        "gold_highlight": (230, 210, 130),  # 金色高光
        "pipe_color": (180, 180, 180),  # 铁色管道
        "pipe_highlight": (200, 200, 200),  # 管道高光
        "glass_color": (220, 220, 220, 150),  # 玻璃效果
        "details": "iron"  # 材质类型
    },
    {
        "name": "500",
        "color": (220, 200, 80),   # 金色
        "accent_color": (180, 160, 50),  # 金色暗部
        "liquid_color": (200, 180, 60),  # 金色液体
        "liquid_highlight": (230, 210, 90),  # 液体高光
        "gold_color": (230, 210, 100),  # 金色装饰
        "gold_highlight": (255, 230, 130),  # 金色高光
        "pipe_color": (210, 190, 70),  # 金色管道
        "pipe_highlight": (230, 210, 100),  # 管道高光
        "glass_color": (240, 220, 100, 150),  # 玻璃效果
        "details": "gold"  # 材质类型
    },
    {
        "name": "1000",
        "color": (80, 200, 120),   # 绿宝石色
        "accent_color": (50, 160, 80),  # 绿宝石色暗部
        "liquid_color": (60, 180, 100),  # 绿宝石色液体
        "liquid_highlight": (100, 220, 140),  # 液体高光
        "gold_color": (200, 180, 100),  # 金色装饰
        "gold_highlight": (230, 210, 130),  # 金色高光
        "pipe_color": (70, 190, 110),  # 绿宝石色管道
        "pipe_highlight": (90, 210, 130),  # 管道高光
        "glass_color": (100, 220, 140, 150),  # 玻璃效果
        "details": "emerald"  # 材质类型
    },
    {
        "name": "2000",
        "color": (80, 120, 200),   # 钻石色
        "accent_color": (50, 80, 160),  # 钻石色暗部
        "liquid_color": (60, 100, 180),  # 钻石色液体
        "liquid_highlight": (100, 140, 220),  # 液体高光
        "gold_color": (200, 180, 100),  # 金色装饰
        "gold_highlight": (230, 210, 130),  # 金色高光
        "pipe_color": (70, 110, 190),  # 钻石色管道
        "pipe_highlight": (90, 130, 210),  # 管道高光
        "glass_color": (100, 140, 220, 150),  # 玻璃效果
        "details": "diamond"  # 材质类型
    }
]

def draw_pixel_tank(tank_info):
    """绘制像素风格的经验储罐"""
    # 创建透明背景的图像
    image = Image.new('RGBA', (TEXTURE_SIZE, TEXTURE_SIZE), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    
    # 获取颜色
    main_color = tank_info["color"]
    accent_color = tank_info["accent_color"]
    liquid_color = tank_info["liquid_color"]
    liquid_highlight = tank_info["liquid_highlight"]
    gold_color = tank_info["gold_color"]
    gold_highlight = tank_info["gold_highlight"]
    pipe_color = tank_info["pipe_color"]
    pipe_highlight = tank_info["pipe_highlight"]
    glass_color = tank_info["glass_color"]
    details = tank_info["details"]
    
    # 绘制储罐主体
    # 底部
    draw.rectangle([6, 20, 25, 25], fill=accent_color)
    # 底部高光
    draw.line([6, 20, 25, 20], fill=tuple(max(0, c+20) for c in accent_color), width=1)
    draw.line([6, 20, 6, 25], fill=tuple(max(0, c+20) for c in accent_color), width=1)
    
    # 主体
    draw.rectangle([5, 10, 26, 20], fill=main_color)
    # 主体高光
    draw.line([5, 10, 26, 10], fill=tuple(max(0, c+30) for c in main_color), width=1)
    draw.line([5, 10, 5, 20], fill=tuple(max(0, c+30) for c in main_color), width=1)
    # 主体暗部
    draw.line([26, 10, 26, 20], fill=tuple(max(0, c-20) for c in main_color), width=1)
    draw.line([5, 20, 26, 20], fill=tuple(max(0, c-20) for c in main_color), width=1)
    
    # 顶部边缘
    draw.rectangle([6, 8, 25, 10], fill=accent_color)
    # 顶部边缘高光
    draw.line([6, 8, 25, 8], fill=tuple(max(0, c+20) for c in accent_color), width=1)
    
    # 顶部盖子
    draw.rectangle([8, 6, 23, 8], fill=gold_color)
    # 顶部盖子高光
    draw.line([8, 6, 23, 6], fill=gold_highlight, width=1)
    draw.line([8, 6, 8, 8], fill=gold_highlight, width=1)
    
    # 绘制液体
    draw.ellipse([9, 9, 22, 12], fill=liquid_color)
    # 液体高光
    draw.ellipse([10, 9, 13, 11], fill=liquid_highlight)
    
    # 绘制金色装饰环
    draw.rectangle([6, 14, 25, 16], fill=gold_color)
    # 装饰环高光
    draw.line([6, 14, 25, 14], fill=gold_highlight, width=1)
    
    # 绘制管道
    # 管道主体
    draw.rectangle([26, 12, 29, 20], fill=pipe_color)
    # 管道弯曲部分
    draw.rectangle([25, 18, 29, 22], fill=pipe_color)
    draw.rectangle([23, 20, 25, 22], fill=pipe_color)
    # 管道高光
    draw.line([26, 12, 26, 20], fill=pipe_highlight, width=1)
    draw.line([29, 12, 29, 20], fill=tuple(max(0, c-20) for c in pipe_color), width=1)
    draw.line([25, 18, 29, 18], fill=pipe_highlight, width=1)
    
    # 绘制支撑脚
    draw.rectangle([8, 25, 10, 28], fill=accent_color)
    draw.rectangle([21, 25, 23, 28], fill=accent_color)
    # 支撑脚高光
    draw.line([8, 25, 10, 25], fill=tuple(max(0, c+20) for c in accent_color), width=1)
    draw.line([8, 25, 8, 28], fill=tuple(max(0, c+20) for c in accent_color), width=1)
    draw.line([21, 25, 23, 25], fill=tuple(max(0, c+20) for c in accent_color), width=1)
    draw.line([21, 25, 21, 28], fill=tuple(max(0, c+20) for c in accent_color), width=1)
    
    # 绘制液体效果（气泡）
    for i in range(3):
        x = 12 + i * 3
        y = 7 - i
        draw.ellipse([x, y, x+1, y+1], fill=liquid_highlight)
    
    # 绘制材质细节
    if details == "iron":
        # 铁材质纹理
        for i in range(3):
            draw.line([8, 12 + i, 23, 12 + i], fill=tuple(max(0, c-10) for c in main_color), width=1)
    elif details == "gold":
        # 金材质纹理
        for i in range(2):
            draw.line([9, 12 + i*2, 22, 12 + i*2], fill=tuple(max(0, c-15) for c in main_color), width=1)
    elif details == "emerald":
        # 绿宝石材质纹理
        for i in range(4):
            draw.ellipse([10 + i*3, 12, 11 + i*3, 13], fill=tuple(max(0, c-20) for c in main_color))
    elif details == "diamond":
        # 钻石材质纹理
        for i in range(3):
            draw.line([10 + i*4, 11, 14 + i*2, 15], fill=tuple(max(0, c-20) for c in main_color), width=1)
            draw.line([14 + i*2, 11, 10 + i*4, 15], fill=tuple(max(0, c-20) for c in main_color), width=1)
    
    # 绘制玻璃效果
    draw.rectangle([7, 9, 24, 13], fill=glass_color)
    
    # 保存图像
    filename = f"experience_tank_{tank_info['name']}.png"
    image.save(filename)
    print(f"生成纹理: {filename}")

# 生成所有储罐纹理
for tank in TANKS:
    draw_pixel_tank(tank)

print("所有纹理生成完成！")
