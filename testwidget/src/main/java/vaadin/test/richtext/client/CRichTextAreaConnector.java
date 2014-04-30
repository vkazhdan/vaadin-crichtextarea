package vaadin.test.richtext.client;

import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.RichTextArea;
import com.vaadin.client.ApplicationConnection;
import com.vaadin.client.Paintable;
import com.vaadin.client.UIDL;
import com.vaadin.client.ui.AbstractFieldConnector;
import com.vaadin.client.ui.ShortcutActionHandler.BeforeShortcutActionListener;
import com.vaadin.shared.ui.Connect;
import com.vaadin.shared.ui.Connect.LoadStyle;
import com.vaadin.shared.util.SharedUtil;
import vaadin.test.richtext.CRichTextArea;

@Connect(value = CRichTextArea.class, loadStyle = LoadStyle.LAZY)
public class CRichTextAreaConnector extends AbstractFieldConnector implements
        Paintable, BeforeShortcutActionListener {

   private static final vaadin.test.richtext.client.CRichTextArea.FontSize[] fontSizesConstants = new vaadin.test.richtext.client.CRichTextArea.FontSize[] {
           vaadin.test.richtext.client.CRichTextArea.FontSize.XX_SMALL, vaadin.test.richtext.client.CRichTextArea.FontSize.X_SMALL,
           vaadin.test.richtext.client.CRichTextArea.FontSize.SMALL, vaadin.test.richtext.client.CRichTextArea.FontSize.MEDIUM,
           vaadin.test.richtext.client.CRichTextArea.FontSize.LARGE, vaadin.test.richtext.client.CRichTextArea.FontSize.X_LARGE,
           vaadin.test.richtext.client.CRichTextArea.FontSize.XX_LARGE };

   /*
    * Last value received from the server
    */
   private String cachedValue = "";

   @Override
   protected void init() {
      getWidget().addBlurHandler(new BlurHandler() {

         @Override
         public void onBlur(BlurEvent event) {
            flush();
         }
      });
   }

   @Override
   public void updateFromUIDL(final UIDL uidl, ApplicationConnection client) {
      getWidget().client = client;
      getWidget().id = uidl.getId();

      if (uidl.hasAttribute("fontName")) {
         RichTextArea.FontSize fontSize = null;

         if (uidl.hasAttribute("fontSize")) {
            int fontSizeValue = uidl.getIntAttribute("fontSize");
            for (RichTextArea.FontSize fontSizesConstant : fontSizesConstants) {
               if (fontSizesConstant.getNumber() == fontSizeValue) {
                  fontSize = fontSizesConstant;
               }
            }
         }

         getWidget().setFont(uidl.getStringAttribute("fontName"), fontSize);
      }

      if (uidl.hasAttribute("insertHtml")) {
         getWidget().insertHtml(uidl.getStringAttribute("insertHtml"));
      }

      if (uidl.hasVariable("text")) {
         String newValue = uidl.getStringVariable("text");
         if (!SharedUtil.equals(newValue, cachedValue)) {
            getWidget().setValue(newValue);
            cachedValue = newValue;
         }
      }

      if (!isRealUpdate(uidl)) {
         return;
      }

      getWidget().setEnabled(isEnabled());
      getWidget().setReadOnly(isReadOnly());
      getWidget().immediate = getState().immediate;
      int newMaxLength = uidl.hasAttribute("maxLength") ? uidl
              .getIntAttribute("maxLength") : -1;
      if (newMaxLength >= 0) {
         if (getWidget().maxLength == -1) {
            getWidget().keyPressHandler = getWidget().rta
                    .addKeyPressHandler(getWidget());
         }
         getWidget().maxLength = newMaxLength;
      } else if (getWidget().maxLength != -1) {
         getWidget().getElement().setAttribute("maxlength", "");
         getWidget().maxLength = -1;
         getWidget().keyPressHandler.removeHandler();
      }

      if (uidl.hasAttribute("selectAll")) {
         getWidget().selectAll();
      }

   }

   @Override
   public void onBeforeShortcutAction(Event e) {
      flush();
   }

   @Override
   public CVRichTextArea getWidget() {
      return (CVRichTextArea) super.getWidget();
   }

   @Override
   public void flush() {
      if (getConnection() != null && getConnectorId() != null) {
         final String html = getWidget().getSanitizedValue();
         if (!html.equals(cachedValue)) {
            cachedValue = html;
            getConnection().updateVariable(getConnectorId(), "text", html,
                    getState().immediate);
         }
      }
   };
}
