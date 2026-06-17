const sharp = require('sharp');
const fs = require('fs');

async function generate() {
    const src = 'app/src/main/res/drawable-nodpi/ussd_pay_logo.png';
    
    // Adaptive foreground sizes (108dp)
    const sizes = {
        'mdpi': 108,
        'hdpi': 162,
        'xhdpi': 216,
        'xxhdpi': 324,
        'xxxhdpi': 432
    };

    for (const [res, size] of Object.entries(sizes)) {
        const dir = `app/src/main/res/mipmap-${res}`;
        if (!fs.existsSync(dir)) fs.mkdirSync(dir, { recursive: true });
        
        // Remove existing files to avoid Resource conflicts
        if (fs.existsSync(`${dir}/ic_launcher.png`)) fs.unlinkSync(`${dir}/ic_launcher.png`);
        if (fs.existsSync(`${dir}/ic_launcher.webp`)) fs.unlinkSync(`${dir}/ic_launcher.webp`);
        if (fs.existsSync(`${dir}/ic_launcher_round.png`)) fs.unlinkSync(`${dir}/ic_launcher_round.png`);
        if (fs.existsSync(`${dir}/ic_launcher_round.webp`)) fs.unlinkSync(`${dir}/ic_launcher_round.webp`);
        if (fs.existsSync(`${dir}/ic_launcher_foreground.png`)) fs.unlinkSync(`${dir}/ic_launcher_foreground.png`);
        if (fs.existsSync(`${dir}/ic_launcher_foreground.webp`)) fs.unlinkSync(`${dir}/ic_launcher_foreground.webp`);
        
        // Make the logo fit entirely inside the 72dp safe zone.
        // If canvas is 108dp, inner size is around 66-70% to add comfortable padding.
        const innerSize = Math.round(size * 0.60); 
        
        // Create circle mask exactly at innerSize
        const circleSvg = Buffer.from(
            `<svg width="${innerSize}" height="${innerSize}">
                <circle cx="${innerSize/2}" cy="${innerSize/2}" r="${innerSize/2}" fill="white" />
            </svg>`
        );
        
        const paddedSize = size;
        
        // Adaptive foreground (transparent background, circular masked logo)
        await sharp(src)
            .resize(innerSize, innerSize)
            .composite([{ input: circleSvg, blend: 'dest-in' }])
            .extend({
                top: Math.floor((paddedSize - innerSize) / 2),
                bottom: Math.ceil((paddedSize - innerSize) / 2),
                left: Math.floor((paddedSize - innerSize) / 2),
                right: Math.ceil((paddedSize - innerSize) / 2),
                background: { r: 0, g: 0, b: 0, alpha: 0 }
            })
            .png()
            .toFile(`${dir}/ic_launcher_foreground.png`);
            
        // Legacy icon (48dp for mdpi)
        const legacySizes = {
            'mdpi': 48,
            'hdpi': 72,
            'xhdpi': 96,
            'xxhdpi': 144,
            'xxxhdpi': 192
        };
        const legSize = legacySizes[res];
        // Circular legacy icon
        await sharp(src)
            .resize(legSize, legSize)
            .composite([{ 
                input: Buffer.from(`<svg width="${legSize}" height="${legSize}"><circle cx="${legSize/2}" cy="${legSize/2}" r="${legSize/2}" fill="white" /></svg>`), 
                blend: 'dest-in' 
            }])
            .png()
            .toFile(`${dir}/ic_launcher_round.png`);
            
        // Squircle / Rounded Square legacy icon
        const cornerSvg = Buffer.from(
            `<svg width="${legSize}" height="${legSize}">
                <rect x="0" y="0" width="${legSize}" height="${legSize}" rx="${legSize*0.15}" ry="${legSize*0.15}" fill="white"/>
            </svg>`
        );
        await sharp(src)
            .resize(legSize, legSize)
            .composite([{ input: cornerSvg, blend: 'dest-in' }])
            .png()
            .toFile(`${dir}/ic_launcher.png`);
    }
}
generate().catch(console.error);
