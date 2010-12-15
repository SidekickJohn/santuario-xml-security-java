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
package org.apache.xml.security.c14n;



import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.xml.security.exceptions.AlgorithmAlreadyRegisteredException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;


/**
 *
 *
 * @author Christian Geuer-Pollmann
 */
public class Canonicalizer {

   //J-
   /** The output encoding of canonicalized data */
   public static final String ENCODING = "UTF8";

   private static final String XPATH_NO_COMMENTS = "[not(self::comment())]";
   public static final String XPATH_C14N_WITH_COMMENTS = "(//. | //@* | //namespace::*)";
   public static final String XPATH_C14N_OMIT_COMMENTS = XPATH_C14N_WITH_COMMENTS + XPATH_NO_COMMENTS;
   public static final String XPATH_C14N_WITH_COMMENTS_SINGLE_NODE = "(.//. | .//@* | .//namespace::*)";
   public static final String XPATH_C14N_OMIT_COMMENTS_SINGLE_NODE = XPATH_C14N_WITH_COMMENTS_SINGLE_NODE + XPATH_NO_COMMENTS;

   public static final String ALGO_ID_C14N_OMIT_COMMENTS = "http://www.w3.org/TR/2001/REC-xml-c14n-20010315";
   public static final String ALGO_ID_C14N_WITH_COMMENTS = ALGO_ID_C14N_OMIT_COMMENTS + "#WithComments";
   public static final String ALGO_ID_C14N_EXCL_OMIT_COMMENTS = "http://www.w3.org/2001/10/xml-exc-c14n#";
   public static final String ALGO_ID_C14N_EXCL_WITH_COMMENTS = ALGO_ID_C14N_EXCL_OMIT_COMMENTS + "WithComments";

   static boolean _alreadyInitialized = false;
   static Map _canonicalizerHash = null;

   protected CanonicalizerSpi canonicalizerSpi = null;
   //J+

   /**
    * Method init
    *
    */
   public static void init() {

      if (!Canonicalizer._alreadyInitialized) {
         Canonicalizer._canonicalizerHash = new HashMap(10);
         Canonicalizer._alreadyInitialized = true;
      }
   }

   /**
    * Constructor Canonicalizer
    *
    * @param algorithmURI
    * @throws InvalidCanonicalizerException
    */
   private Canonicalizer(String algorithmURI)
           throws InvalidCanonicalizerException {

      try {
         String implementingClass = getImplementingClass(algorithmURI);

         this.canonicalizerSpi =
            (CanonicalizerSpi) Class.forName(implementingClass).newInstance();
      } catch (Exception e) {
         Object exArgs[] = { algorithmURI };

         throw new InvalidCanonicalizerException(
            "signature.Canonicalizer.UnknownCanonicalizer", exArgs);
      }
   }

   /**
    * Method getInstance
    *
    * @param algorithmURI
    *
    * @throws InvalidCanonicalizerException
    */
   public static final Canonicalizer getInstance(String algorithmURI)
           throws InvalidCanonicalizerException {

      Canonicalizer c14nizer = new Canonicalizer(algorithmURI);

      return c14nizer;
   }

   /**
    * Method register
    *
    * @param algorithmURI
    * @param implementingClass
    * @throws AlgorithmAlreadyRegisteredException
    */
   public static void register(String algorithmURI, String implementingClass)
           throws AlgorithmAlreadyRegisteredException {

      // check whether URI is already registered
      String registeredClass = getImplementingClass(algorithmURI);

      if ((registeredClass != null) && (registeredClass.length() != 0)) {
         Object exArgs[] = { algorithmURI, registeredClass };

         throw new AlgorithmAlreadyRegisteredException(
            "algorithm.alreadyRegistered", exArgs);
      }

      _canonicalizerHash.put(algorithmURI, implementingClass);
   }

   /**
    * Method getURI
    *
    *
    */
   public final String getURI() {
      return this.canonicalizerSpi.engineGetURI();
   }

   /**
    * Method getIncludeComments
    *
    *
    */
   public boolean getIncludeComments() {
      return this.canonicalizerSpi.engineGetIncludeComments();
   }

   /**
    * This method tries to canonicalize the given bytes. It's possible to even
    * canonicalize non-wellformed sequences if they are well-formed after being
    * wrapped with a <CODE>&gt;a&lt;...&gt;/a&lt;</CODE>.
    *
    * @param inputBytes
    *
    * @throws CanonicalizationException
    * @throws java.io.IOException
    * @throws javax.xml.parsers.ParserConfigurationException
    * @throws org.xml.sax.SAXException
    */
   public byte[] canonicalize(byte[] inputBytes)
           throws javax.xml.parsers.ParserConfigurationException,
                  java.io.IOException, org.xml.sax.SAXException,
                  CanonicalizationException {

      ByteArrayInputStream bais = new ByteArrayInputStream(inputBytes);
      InputSource in = new InputSource(bais);
      DocumentBuilderFactory dfactory = DocumentBuilderFactory.newInstance();

      dfactory.setNamespaceAware(true);

      // needs to validate for ID attribute nomalization
      dfactory.setValidating(true);

      DocumentBuilder db = dfactory.newDocumentBuilder();

      /*
       * for some of the test vectors from the specification,
       * there has to be a validatin parser for ID attributes, default
       * attribute values, NMTOKENS, etc.
       * Unfortunaltely, the test vectors do use different DTDs or
       * even no DTD. So Xerces 1.3.1 fires many warnings about using
       * ErrorHandlers.
       *
       * Text from the spec:
       *
       * The input octet stream MUST contain a well-formed XML document,
       * but the input need not be validated. However, the attribute
       * value normalization and entity reference resolution MUST be
       * performed in accordance with the behaviors of a validating
       * XML processor. As well, nodes for default attributes (declared
       * in the ATTLIST with an AttValue but not specified) are created
       * in each element. Thus, the declarations in the document type
       * declaration are used to help create the canonical form, even
       * though the document type declaration is not retained in the
       * canonical form.
       *
       */
      db.setErrorHandler(new org.apache.xml.security.utils
         .IgnoreAllErrorHandler());

      Document document = db.parse(in);
      byte result[] = this.canonicalizeSubtree(document);

      return result;
   }

   /**
    * Canonicalizes the subtree rooted by <CODE>node</CODE>.
    *
    * @param node
    *
    * @throws CanonicalizationException
    */
   public byte[] canonicalizeSubtree(Node node)
           throws CanonicalizationException {
      return this.canonicalizerSpi.engineCanonicalizeSubTree(node);
   }

   /**
    * Canonicalizes the subtree rooted by <CODE>node</CODE>.
    *
    * @param node
    * @param inclusiveNamespaces
    *
    * @throws CanonicalizationException
    */
   public byte[] canonicalizeSubtree(Node node, String inclusiveNamespaces)
           throws CanonicalizationException {
      return this.canonicalizerSpi.engineCanonicalizeSubTree(node,
              inclusiveNamespaces);
   }

   /**
    * Canonicalizes an XPath node set. The <CODE>xpathNodeSet</CODE> is treated
    * as a list of XPath nodes, not as a list of subtrees.
    *
    * @param xpathNodeSet
    *
    * @throws CanonicalizationException
    */
   public byte[] canonicalizeXPathNodeSet(NodeList xpathNodeSet)
           throws CanonicalizationException {
      return this.canonicalizerSpi.engineCanonicalizeXPathNodeSet(xpathNodeSet);
   }

   /**
    * Canonicalizes an XPath node set. The <CODE>xpathNodeSet</CODE> is treated
    * as a list of XPath nodes, not as a list of subtrees.
    *
    * @param xpathNodeSet
    * @param inclusiveNamespaces
    *
    * @throws CanonicalizationException
    */
   public byte[] canonicalizeXPathNodeSet(
           NodeList xpathNodeSet, String inclusiveNamespaces)
              throws CanonicalizationException {
      return this.canonicalizerSpi.engineCanonicalizeXPathNodeSet(xpathNodeSet,
              inclusiveNamespaces);
   }

   /**
    * Canonicalizes an XPath node set.
    *
    * @param xpathNodeSet
    *
    * @throws CanonicalizationException
    */
   public byte[] canonicalizeXPathNodeSet(Set xpathNodeSet)
           throws CanonicalizationException {
      return this.canonicalizerSpi.engineCanonicalizeXPathNodeSet(xpathNodeSet);
   }

   /**
    * Canonicalizes an XPath node set.
    *
    * @param xpathNodeSet
    * @param inclusiveNamespaces
    *
    * @throws CanonicalizationException
    */
   public byte[] canonicalizeXPathNodeSet(
           Set xpathNodeSet, String inclusiveNamespaces)
              throws CanonicalizationException {
      return this.canonicalizerSpi.engineCanonicalizeXPathNodeSet(xpathNodeSet,
              inclusiveNamespaces);
   }

   /**
    * Returns the name of the implementing {@link CanonicalizerSpi} class
    *
    * @return the name of the implementing {@link CanonicalizerSpi} class
    */
   public String getImplementingCanonicalizerClass() {
      return this.canonicalizerSpi.getClass().getName();
   }

   /**
    * Method getImplementingClass
    *
    * @param URI
    *
    */
   private static String getImplementingClass(String URI) {

      Iterator i = _canonicalizerHash.keySet().iterator();

      while (i.hasNext()) {
         String key = (String) i.next();

         if (key.equals(URI)) {
            return (String) _canonicalizerHash.get(key);
         }
      }

      return null;
   }
}