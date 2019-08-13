/*
 * A11y.java
 *
 * Copyright (C) 2009-19 by RStudio, Inc.
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.core.client.a11y;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.Widget;
import org.rstudio.core.client.StringUtil;

/**
 * Accessibility helpers including newer ARIA stuff not included with GWT
 */
public class A11y
{
   /**
    * Typing delay before updating related aria-live message
    */
   public static int TypingStatusDelayMs = 2000;
   
   public static void setARIADialogModal(Element element)
   {
      element.setAttribute("aria-modal", "true");
   }

   /**
    * Flag an image that does not convey content, is decorative, or is
    * redundant (purpose already conveyed in text).
    * @param element Image element
    */
   public static void setDecorativeImage(Element element)
   {
      element.setAttribute("alt", "");
   }

   public static void setARIAMenuItemExpanded(Element element, boolean expanded)
   {
      // W3C recommends having no aria-expanded state when value is false
      if (expanded)
         element.setAttribute("aria-expanded", "true");
      else
         element.removeAttribute("aria-expanded");
   }

   public static void setARIATablistOrientation(Element element, boolean vertical)
   {
      element.setAttribute("aria-orientation", vertical ? "vertical" : "horizontal");
   }

   /**
    * Associate a label element with another element via the <code>for</code> attribute.
    * @param label the element to add <code>for</code> attribute to (assumed to be label)
    * @param field the element whose id should be used; will add an id if necessary
    */
   public static void associateLabelWithField(Element label, Element field)
   {
      String fieldId = field.getId();
      if (StringUtil.isNullOrEmpty(fieldId))
      {
         fieldId = DOM.createUniqueId();
         field.setId(fieldId);
      }
      label.setAttribute("for", fieldId);
   }

   /**
    * Set aria-current value on an element
    * @param element element to mark
    * @param value value ("page", "step", "location", "date", "time", "true", "false")
    */
   public static void setARIACurrent(Element element, String value)
   {
      if (StringUtil.isNullOrEmpty(value) || StringUtil.equals(value, "false"))
         element.removeAttribute("aria-current");
      else
         element.setAttribute("aria-current", value);
   }
   
   /**
    * Set aria-current value on an element
    * @param widget widget to mark
    * @param value value ("page", "step", "location", "date", "time", "true", "false")
    */
   public static void setARIACurrent(Widget widget, String value)
   {
      setARIACurrent(widget.getElement(), value);
   }

   /**
    * Make a widget hidden to screen readers.
    * @param widget
    */
   public static void setARIAHidden(Widget widget)
   {
      setARIAHidden(widget.getElement());
   }

   /**
    * Make an element hidden to screen readers.
    * @param el
    */
   public static void setARIAHidden(Element el)
   {
      el.setAttribute("aria-hidden", "true");
   }
}
