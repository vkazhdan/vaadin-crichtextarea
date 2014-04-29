package vaadin.test.demo;

import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Title;
import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinServlet;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import vaadin.test.richtext.CRichTextArea;

import javax.servlet.annotation.WebServlet;

@Theme("demo")
@Title("MyCustomComponent Add-on Demo")
@SuppressWarnings("serial")
public class DemoUI extends UI {

   @WebServlet(value = "/*", asyncSupported = true)
   @VaadinServletConfiguration(productionMode = false, ui = DemoUI.class, widgetset = "vaadin.test.demo.DemoWidgetSet")
   public static class Servlet extends VaadinServlet {
   }

   @Override
   protected void init(VaadinRequest request) {

      // Initialize our new UI component
      CRichTextArea component = new CRichTextArea();

      VerticalLayout layout = new VerticalLayout();
      layout.setSizeFull();
      layout.addComponent(component);

      setContent(layout);
   }

}
