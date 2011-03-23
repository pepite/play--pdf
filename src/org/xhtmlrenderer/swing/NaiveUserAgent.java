package org.xhtmlrenderer.swing;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Set;
import javax.imageio.ImageIO;
import org.xhtmlrenderer.event.DocumentListener;
import org.xhtmlrenderer.extend.UserAgentCallback;
import org.xhtmlrenderer.resource.CSSResource;
import org.xhtmlrenderer.resource.ImageResource;
import org.xhtmlrenderer.resource.XMLResource;
import org.xhtmlrenderer.util.XRLog;

import play.Logger;
import play.Play;
import play.vfs.VirtualFile;

public class NaiveUserAgent
  implements UserAgentCallback, DocumentListener
{
  private static final int DEFAULT_IMAGE_CACHE_SIZE = 16;
  protected LinkedHashMap _imageCache;
  private int _imageCacheCapacity;
  private String _baseURL;

  public NaiveUserAgent()
  {
    this(16);
  }

  public NaiveUserAgent(int imgCacheSize)
  {
    this._imageCacheCapacity = imgCacheSize;

    this._imageCache = new LinkedHashMap(this._imageCacheCapacity, 0.75F, true);
  }

  public void shrinkImageCache()
  {
    int ovr = this._imageCache.size() - this._imageCacheCapacity;
    Iterator it = this._imageCache.keySet().iterator();
    while ((it.hasNext()) && (ovr-- > 0)) {
      it.next();
      it.remove();
    }
  }

  public void clearImageCache()
  {
    this._imageCache.clear();
  }

  protected InputStream resolveAndOpenStream(String uri)
  {
    InputStream is = null;
    uri = resolveURI(uri);
    try {
      is = new URL(uri).openStream();
    } catch (MalformedURLException e) {
      XRLog.exception("bad URL given: " + uri, e);
    } catch (FileNotFoundException e) {
      XRLog.exception("item at URI " + uri + " not found");
    } catch (IOException e) {
      XRLog.exception("IO problem for " + uri, e);
    }
    return is;
  }

  public CSSResource getCSSResource(String uri)
  {
    return new CSSResource(resolveAndOpenStream(uri));
  }

  public ImageResource getImageResource(String uri)
  {
    uri = resolveURI(uri);
    ImageResource ir = (ImageResource)this._imageCache.get(uri);

    if (ir == null) {
      InputStream is = resolveAndOpenStream(uri);
      if (is != null)
        try {
          BufferedImage img = ImageIO.read(is);
          if (img == null) {
            throw new IOException("ImageIO.read() returned null");
          }
          ir = createImageResource(uri, img);
          this._imageCache.put(uri, ir);
        } catch (FileNotFoundException e) {
          XRLog.exception("Can't read image file; image at URI '" + uri + "' not found");
        } catch (IOException e) {
          XRLog.exception("Can't read image file; unexpected problem for URI '" + uri + "'", e);
        } finally {
          try {
            is.close();
          }
          catch (IOException e)
          {
          }
        }
    }
    if (ir == null) {
      ir = createImageResource(uri, null);
    }
    return ir;
  }

  protected ImageResource createImageResource(String uri, Image img)
  {
    return new ImageResource(uri, AWTFSImage.createImage(img));
  }
  public XMLResource getXMLResource(String uri) {
    InputStream inputStream = resolveAndOpenStream(uri);
    XMLResource xmlResource;
    try {
      xmlResource = XMLResource.load(inputStream);
    } finally {
      if (inputStream != null) try {
          inputStream.close();
        }
        catch (IOException e)
        {
        } 
    }
    return xmlResource;
  }

  public byte[] getBinaryResource(String uri) {
    InputStream is = resolveAndOpenStream(uri);
    try {
      ByteArrayOutputStream result = new ByteArrayOutputStream();
      byte[] buf = new byte[10240];
      int i;
      while ((i = is.read(buf)) != -1) {
        result.write(buf, 0, i);
      }
      is.close();
      is = null;

      byte[] arrayOfByte1 = result.toByteArray();
      return arrayOfByte1;
    }
    catch (IOException e)
    {
      return null;
    }
    finally
    {
      if (is != null)
        try {
          is.close(); } catch (IOException e) {
        }
    }
  }

  public boolean isVisited(String uri)
  {
    return false;
  }

  public void setBaseURL(String url)
  {
    this._baseURL = url;
  }

  public String resolveURI(String uri)
  {
    if (uri == null) return null;
    String ret = null;
    if (this._baseURL == null) {
      try {
        URL result = new URL(uri);
        setBaseURL(result.toExternalForm());
      } catch (MalformedURLException e) {
        try {
          setBaseURL(new File(".").toURI().toURL().toExternalForm());
        } catch (Exception e1) {
          XRLog.exception("The default NaiveUserAgent doesn't know how to resolve the base URL for " + uri);
          return null;
        }
      }
    }
    try
    {
    	// try to find it in play
    	VirtualFile file = Play.getVirtualFile(uri);
    	Logger.debug("Resolved uri %s to file %s", uri, file);
    	if(file != null && file.exists())
    		return file.getRealFile().toURI().toURL().toExternalForm();
      return new URL(uri).toString();
    } catch (MalformedURLException e) {
      XRLog.load(uri + " is not a URL; may be relative. Testing using parent URL " + this._baseURL);
      try {
        URL result = new URL(new URL(this._baseURL), uri);
        ret = result.toString();
      } catch (MalformedURLException e1) {
        XRLog.exception("The default NaiveUserAgent cannot resolve the URL " + uri + " with base URL " + this._baseURL);
      }
    }
    return ret;
  }

  public String getBaseURL()
  {
    return this._baseURL;
  }

  public void documentStarted() {
    shrinkImageCache();
  }

  public void documentLoaded()
  {
  }

  public void onLayoutException(Throwable t)
  {
  }

  public void onRenderException(Throwable t)
  {
  }
}