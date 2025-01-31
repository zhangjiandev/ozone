/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.apache.hadoop.hdds.security.x509.certificate.utils;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.hadoop.hdds.conf.OzoneConfiguration;
import org.apache.hadoop.hdds.security.exception.SCMSecurityException;
import org.apache.hadoop.hdds.security.x509.SecurityConfig;
import org.apache.hadoop.hdds.security.x509.certificates.utils.SelfSignedCertificate;
import org.apache.hadoop.hdds.security.x509.keys.HDDSKeyGenerator;
import org.bouncycastle.cert.X509CertificateHolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import static org.apache.hadoop.hdds.HddsConfigKeys.OZONE_METADATA_DIRS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the Certificate codecs.
 */
public class TestCertificateCodec {
  private static OzoneConfiguration conf = new OzoneConfiguration();
  private static final String COMPONENT = "test";
  private SecurityConfig securityConfig;

  @BeforeEach
  public void init(@TempDir Path tempDir) {
    conf.set(OZONE_METADATA_DIRS, tempDir.toString());
    securityConfig = new SecurityConfig(conf);
  }

  /**
   * This test converts a X509Certificate Holder object to a PEM encoded String,
   * then creates a new X509Certificate object to verify that we are able to
   * serialize and deserialize correctly. we follow up with converting these
   * objects to standard JCA x509Certificate objects.
   *
   * @throws NoSuchProviderException  - on Error.
   * @throws NoSuchAlgorithmException - on Error.
   * @throws IOException              - on Error.
   * @throws SCMSecurityException     - on Error.
   * @throws CertificateException     - on Error.
   */
  @Test
  public void testGetPEMEncodedString()
      throws NoSuchProviderException, NoSuchAlgorithmException,
      IOException, SCMSecurityException, CertificateException {
    HDDSKeyGenerator keyGenerator =
        new HDDSKeyGenerator(conf);
    X509CertificateHolder cert =
        SelfSignedCertificate.newBuilder()
            .setSubject(RandomStringUtils.randomAlphabetic(4))
            .setClusterID(RandomStringUtils.randomAlphabetic(4))
            .setScmID(RandomStringUtils.randomAlphabetic(4))
            .setBeginDate(LocalDate.now())
            .setEndDate(LocalDate.now().plus(1, ChronoUnit.DAYS))
            .setConfiguration(keyGenerator.getSecurityConfig()
                .getConfiguration())
            .setKey(keyGenerator.generateKey())
            .makeCA()
            .build();
    CertificateCodec codec = new CertificateCodec(securityConfig, COMPONENT);
    String pemString = codec.getPEMEncodedString(cert);
    assertTrue(pemString.startsWith(CertificateCodec.BEGIN_CERT));
    assertTrue(pemString.endsWith(CertificateCodec.END_CERT + "\n"));

    // Read back the certificate and verify that all the comparisons pass.
    X509CertificateHolder newCert =
        codec.getCertificateHolder(codec.getX509Certificate(pemString));
    assertEquals(cert, newCert);

    // Just make sure we can decode both these classes to Java Std. lIb classes.
    X509Certificate firstCert = CertificateCodec.getX509Certificate(cert);
    X509Certificate secondCert = CertificateCodec.getX509Certificate(newCert);
    assertEquals(firstCert, secondCert);
  }

  /**
   * tests writing and reading certificates in PEM encoded form.
   *
   * @throws NoSuchProviderException  - on Error.
   * @throws NoSuchAlgorithmException - on Error.
   * @throws IOException              - on Error.
   * @throws SCMSecurityException     - on Error.
   * @throws CertificateException     - on Error.
   */
  @Test
  public void testWriteCertificate(@TempDir Path basePath)
      throws NoSuchProviderException, NoSuchAlgorithmException,
      IOException, SCMSecurityException, CertificateException {
    HDDSKeyGenerator keyGenerator =
        new HDDSKeyGenerator(conf);
    X509CertificateHolder cert =
        SelfSignedCertificate.newBuilder()
            .setSubject(RandomStringUtils.randomAlphabetic(4))
            .setClusterID(RandomStringUtils.randomAlphabetic(4))
            .setScmID(RandomStringUtils.randomAlphabetic(4))
            .setBeginDate(LocalDate.now())
            .setEndDate(LocalDate.now().plus(1, ChronoUnit.DAYS))
            .setConfiguration(keyGenerator.getSecurityConfig()
                .getConfiguration())
            .setKey(keyGenerator.generateKey())
            .makeCA()
            .build();
    CertificateCodec codec = new CertificateCodec(securityConfig, COMPONENT);
    String pemString = codec.getPEMEncodedString(cert);
    codec.writeCertificate(basePath, "pemcertificate.crt",
        pemString, false);
    X509CertificateHolder certHolder =
        codec.readCertificate(basePath, "pemcertificate.crt");
    assertNotNull(certHolder);
    assertEquals(cert.getSerialNumber(),
        certHolder.getSerialNumber());
  }

  /**
   * Tests reading and writing certificates in DER form.
   *
   * @throws IOException              - on Error.
   * @throws SCMSecurityException     - on Error.
   * @throws CertificateException     - on Error.
   * @throws NoSuchProviderException  - on Error.
   * @throws NoSuchAlgorithmException - on Error.
   */
  @Test
  public void testwriteCertificateDefault()
      throws IOException, SCMSecurityException, CertificateException,
      NoSuchProviderException, NoSuchAlgorithmException {
    HDDSKeyGenerator keyGenerator =
        new HDDSKeyGenerator(conf);
    X509CertificateHolder cert =
        SelfSignedCertificate.newBuilder()
            .setSubject(RandomStringUtils.randomAlphabetic(4))
            .setClusterID(RandomStringUtils.randomAlphabetic(4))
            .setScmID(RandomStringUtils.randomAlphabetic(4))
            .setBeginDate(LocalDate.now())
            .setEndDate(LocalDate.now().plus(1, ChronoUnit.DAYS))
            .setConfiguration(keyGenerator.getSecurityConfig()
                .getConfiguration())
            .setKey(keyGenerator.generateKey())
            .makeCA()
            .build();
    CertificateCodec codec = new CertificateCodec(securityConfig, COMPONENT);
    codec.writeCertificate(cert);
    X509CertificateHolder certHolder = codec.readCertificate();
    assertNotNull(certHolder);
    assertEquals(cert.getSerialNumber(), certHolder.getSerialNumber());
  }

  /**
   * Tests writing to non-default certificate file name.
   *
   * @throws IOException              - on Error.
   * @throws SCMSecurityException     - on Error.
   * @throws NoSuchProviderException  - on Error.
   * @throws NoSuchAlgorithmException - on Error.
   * @throws CertificateException     - on Error.
   */
  @Test
  public void writeCertificate2() throws IOException, SCMSecurityException,
      NoSuchProviderException, NoSuchAlgorithmException, CertificateException {
    HDDSKeyGenerator keyGenerator =
        new HDDSKeyGenerator(conf);
    X509CertificateHolder cert =
        SelfSignedCertificate.newBuilder()
            .setSubject(RandomStringUtils.randomAlphabetic(4))
            .setClusterID(RandomStringUtils.randomAlphabetic(4))
            .setScmID(RandomStringUtils.randomAlphabetic(4))
            .setBeginDate(LocalDate.now())
            .setEndDate(LocalDate.now().plus(1, ChronoUnit.DAYS))
            .setConfiguration(keyGenerator.getSecurityConfig()
                .getConfiguration())
            .setKey(keyGenerator.generateKey())
            .makeCA()
            .build();
    CertificateCodec codec =
        new CertificateCodec(keyGenerator.getSecurityConfig(), "ca");
    codec.writeCertificate(cert, "newcert.crt", false);
    // Rewrite with force support
    codec.writeCertificate(cert, "newcert.crt", true);
    X509CertificateHolder x509CertificateHolder =
        codec.readCertificate(codec.getLocation(), "newcert.crt");
    assertNotNull(x509CertificateHolder);

  }
}