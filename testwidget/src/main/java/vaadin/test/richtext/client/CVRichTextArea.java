package vaadin.test.richtext.client;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Focusable;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.RichTextArea;
import com.google.gwt.user.client.ui.Widget;
import com.vaadin.client.ApplicationConnection;
import com.vaadin.client.BrowserInfo;
import com.vaadin.client.ConnectorMap;
import com.vaadin.client.Util;
import com.vaadin.client.ui.Field;
import com.vaadin.client.ui.ShortcutActionHandler;
import com.vaadin.client.ui.ShortcutActionHandler.ShortcutActionHandlerOwner;
import com.vaadin.client.ui.TouchScrollDelegate;
import com.vaadin.client.ui.richtextarea.VRichTextToolbar;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * This class implements a basic client side rich text editor component.
 *
 * @author Vaadin Ltd.
 */
public class CVRichTextArea extends Composite implements Field, KeyPressHandler,
        KeyDownHandler, Focusable {

   /**
    * The input node CSS classname.
    */
   public static final String CLASSNAME = "v-richtextarea";

   /**
    * For internal use only. May be removed or replaced in the future.
    */
   public String id;

   /**
    * For internal use only. May be removed or replaced in the future.
    */
   public ApplicationConnection client;

   /**
    * For internal use only. May be removed or replaced in the future.
    */
   public boolean immediate = false;

   /**
    * For internal use only. May be removed or replaced in the future.
    */
   public CRichTextArea rta;

   private VRichTextToolbar formatter;

   /**
    * For internal use only. May be removed or replaced in the future.
    */
   public HTML html = new HTML();

   private final FlowPanel fp = new FlowPanel();

   private boolean enabled = true;

   private int extraHorizontalPixels = -1;
   private int extraVerticalPixels = -1;

   /**
    * For internal use only. May be removed or replaced in the future.
    */
   public int maxLength = -1;

   private int toolbarNaturalWidth = 500;

   /**
    * For internal use only. May be removed or replaced in the future.
    */
   public HandlerRegistration keyPressHandler;

   private ShortcutActionHandlerOwner hasShortcutActionHandler;

   private boolean readOnly = false;

   private final Map<BlurHandler, HandlerRegistration> blurHandlers = new HashMap<BlurHandler, HandlerRegistration>();

   public CVRichTextArea() {
      createRTAComponents();
      fp.add(formatter);
      fp.add(rta);

      initWidget(fp);
      setStyleName(CLASSNAME);

      TouchScrollDelegate.enableTouchScrolling(html, html.getElement());
   }

   private void createRTAComponents() {
      rta = new CRichTextArea();
      rta.setWidth("100%");
      rta.addKeyDownHandler(this);
      formatter = new VRichTextToolbar(rta);

      // Add blur handlers
      for (Entry<BlurHandler, HandlerRegistration> handler : blurHandlers
              .entrySet()) {

         // Remove old registration
         handler.getValue().removeHandler();

         // Add blur handlers
         addBlurHandler(handler.getKey());
      }
   }

   public void setEnabled(boolean enabled) {
      if (this.enabled != enabled) {
         // rta.setEnabled(enabled);
         swapEditableArea();
         this.enabled = enabled;
      }
   }

   /**
    * Swaps html to rta and visa versa.
    */
   private void swapEditableArea() {
      String value = getValue();
      if (html.isAttached()) {
         fp.remove(html);
         if (BrowserInfo.get().isWebkit()) {
            fp.remove(formatter);
            createRTAComponents(); // recreate new RTA to bypass #5379
            fp.add(formatter);
         }
         fp.add(rta);
      } else {
         fp.remove(rta);
         fp.add(html);
      }
      setValue(value);
   }

   /**
    * For internal use only. May be removed or replaced in the future.
    */
   public void selectAll() {
        /*
         * There is a timing issue if trying to select all immediately on first
         * render. Simple deferred command is not enough. Using Timer with
         * moderated timeout. If this appears to fail on many (most likely slow)
         * environments, consider increasing the timeout.
         *
         * FF seems to require the most time to stabilize its RTA. On Vaadin
         * tiergarden test machines, 200ms was not enough always (about 50%
         * success rate) - 300 ms was 100% successful. This however was not
         * enough on a sluggish old non-virtualized XP test machine. A bullet
         * proof solution would be nice, GWT 2.1 might however solve these. At
         * least setFocus has a workaround for this kind of issue.
         */
      new Timer() {
         @Override
         public void run() {
            rta.getFormatter().selectAll();
         }
      }.schedule(320);
   }

   public void setReadOnly(boolean b) {
      if (isReadOnly() != b) {
         swapEditableArea();
         readOnly = b;
      }
      // reset visibility in case enabled state changed and the formatter was
      // recreated
      formatter.setVisible(!readOnly);
   }

   private boolean isReadOnly() {
      return readOnly;
   }

   /**
    * @return space used by components paddings and borders
    */
   private int getExtraHorizontalPixels() {
      if (extraHorizontalPixels < 0) {
         detectExtraSizes();
      }
      return extraHorizontalPixels;
   }

   /**
    * @return space used by components paddings and borders
    */
   private int getExtraVerticalPixels() {
      if (extraVerticalPixels < 0) {
         detectExtraSizes();
      }
      return extraVerticalPixels;
   }

   /**
    * Detects space used by components paddings and borders.
    */
   private void detectExtraSizes() {
      Element clone = Util.cloneNode(getElement(), false);
      DOM.setElementAttribute(clone, "id", "");
      DOM.setStyleAttribute(clone, "visibility", "hidden");
      DOM.setStyleAttribute(clone, "position", "absolute");
      // due FF3 bug set size to 10px and later subtract it from extra pixels
      DOM.setStyleAttribute(clone, "width", "10px");
      DOM.setStyleAttribute(clone, "height", "10px");
      DOM.appendChild(DOM.getParent(getElement()), clone);
      extraHorizontalPixels = DOM.getElementPropertyInt(clone, "offsetWidth") - 10;
      extraVerticalPixels = DOM.getElementPropertyInt(clone, "offsetHeight") - 10;

      DOM.removeChild(DOM.getParent(getElement()), clone);
   }

   @Override
   public void setHeight(String height) {
      if (height.endsWith("px")) {
         float h = Float
                 .parseFloat(height.substring(0, height.length() - 2));
         h -= getExtraVerticalPixels();
         if (h < 0) {
            h = 0;
         }

         super.setHeight(h + "px");
      } else {
         super.setHeight(height);
      }

      if (height == null || height.equals("")) {
         rta.setHeight("");
      } else {
            /*
             * The formatter height will be initially calculated wrong so we
             * delay the height setting so the DOM has had time to stabilize.
             */
         Scheduler.get().scheduleDeferred(new Command() {
            @Override
            public void execute() {
               int editorHeight = getOffsetHeight()
                       - getExtraVerticalPixels()
                       - formatter.getOffsetHeight();
               if (editorHeight < 0) {
                  editorHeight = 0;
               }
               rta.setHeight(editorHeight + "px");
            }
         });
      }
   }

   @Override
   public void setWidth(String width) {
      if (width.endsWith("px")) {
         float w = Float.parseFloat(width.substring(0, width.length() - 2));
         w -= getExtraHorizontalPixels();
         if (w < 0) {
            w = 0;
         }

         super.setWidth(w + "px");
      } else if (width.equals("")) {
            /*
             * IE cannot calculate the width of the 100% iframe correctly if
             * there is no width specified for the parent. In this case we would
             * use the toolbar but IE cannot calculate the width of that one
             * correctly either in all cases. So we end up using a default width
             * for a RichTextArea with no width definition in all browsers (for
             * compatibility).
             */

         super.setWidth(toolbarNaturalWidth + "px");
      } else {
         super.setWidth(width);
      }
   }

   @Override
   public void onKeyPress(KeyPressEvent event) {
      if (maxLength >= 0) {
         Scheduler.get().scheduleDeferred(new Command() {
            @Override
            public void execute() {
               if (rta.getHTML().length() > maxLength) {
                  rta.setHTML(rta.getHTML().substring(0, maxLength));
               }
            }
         });
      }
   }

   @Override
   public void onKeyDown(KeyDownEvent event) {
      // delegate to closest shortcut action handler
      // throw event from the iframe forward to the shortcuthandler
      ShortcutActionHandler shortcutHandler = getShortcutHandlerOwner()
              .getShortcutActionHandler();
      if (shortcutHandler != null) {
         shortcutHandler
                 .handleKeyboardEvent(com.google.gwt.user.client.Event
                                 .as(event.getNativeEvent()),
                         ConnectorMap.get(client).getConnector(this)
                 );
      }
   }

   private ShortcutActionHandlerOwner getShortcutHandlerOwner() {
      if (hasShortcutActionHandler == null) {
         Widget parent = getParent();
         while (parent != null) {
            if (parent instanceof ShortcutActionHandlerOwner) {
               break;
            }
            parent = parent.getParent();
         }
         hasShortcutActionHandler = (ShortcutActionHandlerOwner) parent;
      }
      return hasShortcutActionHandler;
   }

   @Override
   public int getTabIndex() {
      return rta.getTabIndex();
   }

   @Override
   public void setAccessKey(char key) {
      rta.setAccessKey(key);
   }

   @Override
   public void setFocus(boolean focused) {
        /*
         * Similar issue as with selectAll. Focusing must happen before possible
         * selectall, so keep the timeout here lower.
         */
      new Timer() {

         @Override
         public void run() {
            rta.setFocus(true);
         }
      }.schedule(300);
   }

   @Override
   public void setTabIndex(int index) {
      rta.setTabIndex(index);
   }

   /**
    * Set the value of the text area
    *
    * @param value The text value. Can be html.
    */
   public void setValue(String value) {
      if (rta.isAttached()) {
         rta.setHTML(value);
      } else {
         html.setHTML(value);
      }
   }

   /**
    * Get the value the text area
    */
   public String getValue() {
      if (rta.isAttached()) {
         return rta.getHTML();
      } else {
         return html.getHTML();
      }
   }

   /**
    * Browsers differ in what they return as the content of a visually empty
    * rich text area. This method is used to normalize these to an empty
    * string. See #8004.
    *
    * @return cleaned html string
    */
   public String getSanitizedValue() {
      BrowserInfo browser = BrowserInfo.get();
      String result = getValue();
      if (browser.isFirefox()) {
         if ("<br>".equals(result)) {
            result = "";
         }
      } else if (browser.isWebkit()) {
         if ("<br>".equals(result) || "<div><br></div>".equals(result)) {
            result = "";
         }
      } else if (browser.isIE()) {
         if ("<P>&nbsp;</P>".equals(result)) {
            result = "";
         }
      } else if (browser.isOpera()) {
         if ("<br>".equals(result) || "<p><br></p>".equals(result)) {
            result = "";
         }
      }
      return result;
   }

   /**
    * Adds a blur handler to the component.
    *
    * @param blurHandler the blur handler to add
    */
   public void addBlurHandler(BlurHandler blurHandler) {
      blurHandlers.put(blurHandler, rta.addBlurHandler(blurHandler));
   }

   /**
    * Removes a blur handler.
    *
    * @param blurHandler the handler to remove
    */
   public void removeBlurHandler(BlurHandler blurHandler) {
      HandlerRegistration registration = blurHandlers.remove(blurHandler);
      if (registration != null) {
         registration.removeHandler();
      }
   }

   public void setFont(final String fontName, final RichTextArea.FontSize fontSize) {
      Scheduler.get().scheduleFixedDelay(new Scheduler.RepeatingCommand() { // Need a delay for Firefox browser
         @Override
         public boolean execute() {
            rta.getFormatter().setFontName(fontName);

            if (fontSize != null) {
               rta.getFormatter().setFontSize(fontSize);
            }

            return false;
         }
      }, 100);
   }

   public void insertHtml(final String html) {
      Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
         @Override
         public void execute() {
            rta.getFormatter().insertHTML(html);
         }
      });
   }
}

