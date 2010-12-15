/*
 * Copyright  1999-2004 The Apache Software Foundation.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.apache.xml.security.signature;



import javax.xml.transform.TransformerException;

import org.apache.xml.security.exceptions.XMLSecurityException;
import org.apache.xml.security.utils.Constants;
import org.apache.xml.security.utils.IdResolver;
import org.apache.xml.security.utils.SignatureElementProxy;
import org.apache.xml.security.utils.XMLUtils;
import org.apache.xpath.XPathAPI;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


/**
 * Handles <code>&lt;ds:SignatureProperties&gt;</code> elements
 * This Element holds {@link SignatureProperty} that contian additional information items
 * concerning the generation of the signature.
 * for example, data-time stamp, serial number of cryptographic hardware.
 *
 * @author Christian Geuer-Pollmann
 *
 */
public class SignatureProperties extends SignatureElementProxy {

   /** {@link org.apache.commons.logging} logging facility */
    static org.apache.commons.logging.Log log = 
        org.apache.commons.logging.LogFactory.getLog(SignatureProperties.class.getName());

   /**
    * Constructor SignatureProperties
    *
    * @param doc
    */
   public SignatureProperties(Document doc) {

      super(doc);

      XMLUtils.addReturnToElement(this._constructionElement);
   }

   /**
    * Constructs {@link SignatureProperties} from {@link Element}
    * @param element <code>SignatureProperties</code> elementt
    * @param BaseURI the URI of the resource where the XML instance was stored
    * @throws XMLSecurityException
    */
   public SignatureProperties(Element element, String BaseURI)
           throws XMLSecurityException {
      super(element, BaseURI);
   }

   /**
    * Return the nonnegative number of added SignatureProperty elements.
    *
    * @return the number of SignatureProperty elements
    * @throws XMLSignatureException
    */
   public int getLength() throws XMLSignatureException {

      try {
         Element nscontext = XMLUtils.createDSctx(this._doc, "ds",
                                                  Constants.SignatureSpecNS);
         NodeList propertyElems =
            XPathAPI.selectNodeList(this._constructionElement,
                                    "./ds:" + Constants._TAG_SIGNATUREPROPERTY,
                                    nscontext);

         return propertyElems.getLength();
      } catch (TransformerException ex) {
         throw new XMLSignatureException("empty", ex);
      }
   }

   /**
    * Return the <it>i</it><sup>th</sup> SignatureProperty.  Valid <code>i</code>
    * values are 0 to <code>{link@ getSize}-1</code>.
    *
    * @param i Index of the requested {@link SignatureProperty}
    * @return the <it>i</it><sup>th</sup> SignatureProperty
    * @throws XMLSignatureException
    */
   public SignatureProperty item(int i) throws XMLSignatureException {

      try {
         Element nscontext = XMLUtils.createDSctx(this._doc, "ds",
                                                  Constants.SignatureSpecNS);
         Element propertyElem =
            (Element) XPathAPI
               .selectSingleNode(this._constructionElement, "./ds:"
                                 + Constants._TAG_SIGNATUREPROPERTY + "["
                                 + (i + 1) + "]", nscontext);

         if (propertyElem == null) {
            return null;
         } else {
            return new SignatureProperty(propertyElem, this._baseURI);
         }
      } catch (TransformerException ex) {
         throw new XMLSignatureException("empty", ex);
      } catch (XMLSecurityException ex) {
         throw new XMLSignatureException("empty", ex);
      }
   }

   /**
    * Sets the <code>Id</code> attribute
    *
    * @param Id the <code>Id</code> attribute
    */
   public void setId(String Id) {

      if ((this._state == MODE_SIGN) && (Id != null)) {
         this._constructionElement.setAttributeNS(null, Constants._ATT_ID, Id);
         IdResolver.registerElementById(this._constructionElement, Id);
      }
   }

   /**
    * Returns the <code>Id</code> attribute
    *
    * @return the <code>Id</code> attribute
    */
   public String getId() {
      return this._constructionElement.getAttributeNS(null, Constants._ATT_ID);
   }

   /**
    * Method addSignatureProperty
    *
    * @param sp
    */
   public void addSignatureProperty(SignatureProperty sp) {
      this._constructionElement.appendChild(sp.getElement());
      XMLUtils.addReturnToElement(this._constructionElement);
   }

   public String getBaseLocalName() {
      return Constants._TAG_SIGNATUREPROPERTIES;
   }
}