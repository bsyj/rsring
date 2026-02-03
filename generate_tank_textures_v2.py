from PIL import Image, ImageDraw

# 设置纹理尺寸
TEXTURE_SIZE = 32

# 定义等级对应的颜色和材质
TANKS = [
    {
        "name": "100",
        "color": (160, 160, 160),  # 铁色
        "accent_color": (120, 120, 120),  # 铁色暗部
        "liquid_color": (100, 100, 100),  # 铁色液体
        "glass_color": (200, 200, 200, 200),  # 铁色玻璃
        "base_color": (80, 80, 80)  # 铁色底座
    },
    {
        "name": "500",
        "color": (220, 200, 80),   # 金色
        "accent_color": (180, 160, 60),  # 金色暗部
        "liquid_color": (160, 140, 40),  # 金色液体
        "glass_color": (230, 210, 100, 200),  # 金色玻璃
        "base_color": (140, 120, 40)  # 金色底座
    },
    {
        "name": "1000",
        "color": (80, 200, 120),   # 绿宝石色
        "accent_color": (60, 180, 100),  # 绿宝石色暗部
        "liquid_color": (40, 160, 80),  # 绿宝石色液体
        "glass_color": (100, 210, 140, 200),  # 绿宝石色玻璃
        "base_color": (40, 140, 80)  # 绿宝石色底座
    },
    {
        "name": "2000",
        "color": (80, 120, 200),   # 钻石色
        "accent_color": (60, 100, 180),  # 钻石色暗部
        "liquid_color": (40, 80, 160),  # 钻石色液体
        "glass_color": (100, 140, 210, 200),  # 钻石色玻璃
        "base_color": (40, 80, 140)  # 钻石色底座
    }
]

def draw_pixel_tank(tank_info):
    """绘制像素风格的经验储罐，参考提供的蓝色储罐图标"""
    # 创建透明背景的图像
    image = Image.new('RGBA', (TEXTURE_SIZE, TEXTURE_SIZE), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    
    # 获取颜色
    main_color = tank_info["color"]
    accent_color = tank_info["accent_color"]
    liquid_color = tank_info["liquid_color"]
    glass_color = tank_info["glass_color"]
    base_color = tank_info["base_color"]
    
    # 绘制底座
    draw.rectangle([8, 24, 23, 28], fill=base_color)
    
    # 绘制储罐主体
    # 底部边缘
    draw.rectangle([7, 22, 24, 24], fill=accent_color)
    # 主体
    draw.rectangle([7, 10, 24, 22], fill=glass_color)
    # 顶部边缘
    draw.rectangle([8, 8, 23, 10], fill=accent_color)
    
    # 绘制液体
    draw.rectangle([9, 14, 22, 21], fill=liquid_color)
    
    # 绘制顶部开口
    draw.rectangle([10, 6, 21, 8], fill=accent_color)
    draw.rectangle([11, 4, 20, 6], fill=main_color)
    
    # 绘制液体表面
    draw.ellipse([10, 13, 21, 15], fill=liquid_color)
    
    # 绘制高光效果
    # 玻璃高光
    draw.line([12, 10, 18, 16], fill=(255, 255, 255, 150), width=1)
    # 液体高光
    draw.line([14, 16, 19, 19], fill=(255, 255, 255, 150), width=1)
    
    # 保存图像
    filename = f"experience_tank_{tank_info['name']}.png"
    image.save(filename)
    print(f"生成纹理: {filename}")

# 生成所有储罐纹理
for tank in TANKS:
    draw_pixel_tank(tank)

print("所有纹理生成完成！")
