package com.robonobo.midas.thumb;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ThumbManager {
	File imagesDir;
	Log log = LogFactory.getLog(getClass());
	
	public ThumbManager(File imagesDir) {
		this.imagesDir = imagesDir;
	}
	
	public File getFileForId(String id) {
		return new File(imagesDir, id + ".png");
	}
	
	public BufferedImage getImageForId(String id) throws IOException {
		BufferedImage image = null;
		if(imagesDir != null) {
			File loc = getFileForId(id);
			log.info("Looking for an image in path " + loc.getAbsoluteFile());
			if(loc.exists()) {
				image = ImageIO.read(loc);
			} 
		}	
		
		// if no image has been loaded
		if(image == null) {
			InputStream in = getClass().getClassLoader().getResourceAsStream("/com/echostream/midas/servlet/channel-default.png");
			image = ImageIO.read(in);
		}
		
		return image;
	}
	
	public void putImageForId(String id, BufferedImage image) throws IOException {
		ImageIO.write((RenderedImage)image, "png", getFileForId(id));
	}
	

	public void writeThumbToResponse(String id, HttpServletResponse response) throws IOException {
		writeThumbToResponse(id, response, 0);
	}
	
	public void writeThumbToResponse(String id, HttpServletResponse response,int size) throws IOException {
		// requesting the channel image
		response.setContentType("image/png");
		
		BufferedImage image = getImageForId(id);

		if(size>0) {
			image = createResizedCopy(image, size, size, true);
		}
		ImageIO.write((RenderedImage)image, "png", response.getOutputStream());
	}
	
	protected BufferedImage createResizedCopy(Image originalImage, int scaledWidth, int scaledHeight, boolean preserveAlpha)
	{
		int imageType = preserveAlpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
		BufferedImage scaledBI = new BufferedImage(scaledWidth, scaledHeight, imageType);
		Graphics2D g = scaledBI.createGraphics();
		if (preserveAlpha) {
			g.setComposite(AlphaComposite.Src);
		}
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC); 
		g.drawImage(originalImage, 0, 0, scaledWidth, scaledHeight, null); 
		g.dispose();
		return scaledBI;
	}
}
