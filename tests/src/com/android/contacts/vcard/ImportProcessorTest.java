/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.contacts.vcard;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.test.AndroidTestCase;
import android.text.TextUtils;
import android.util.Log;

import com.android.contacts.vcard.ImportProcessor.CommitterGenerator;
import com.android.vcard.VCardEntryCommitter;
import com.android.vcard.VCardInterpreter;
import com.android.vcard.VCardSourceDetector;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.List;

public class ImportProcessorTest extends AndroidTestCase {
    private static final String LOG_TAG = "ImportProcessorTest";
    private ImportProcessor mImportProcessor;

    private String mCopiedFileName;

    // XXX: better way to copy stream?
    private Uri copyToLocal(final String fileName) throws IOException {
        final Context context = getContext();
        // We need to use Context of this unit test runner (not of test to be tested),
        // as only the former knows assets to be copied.
        final Context testContext = getTestContext();
        final ContentResolver resolver = testContext.getContentResolver();
        mCopiedFileName = fileName;
        ReadableByteChannel inputChannel = null;
        WritableByteChannel outputChannel = null;
        Uri destUri;
        try {
            inputChannel = Channels.newChannel(testContext.getAssets().open(fileName));
            destUri = Uri.parse(context.getFileStreamPath(fileName).toURI().toString());
            outputChannel =
                    getContext().openFileOutput(fileName,
                            Context.MODE_WORLD_WRITEABLE).getChannel();
            final ByteBuffer buffer = ByteBuffer.allocateDirect(8192);
            while (inputChannel.read(buffer) != -1) {
                buffer.flip();
                outputChannel.write(buffer);
                buffer.compact();
            }
            buffer.flip();
            while (buffer.hasRemaining()) {
                outputChannel.write(buffer);
            }
        } finally {
            if (inputChannel != null) {
                try {
                    inputChannel.close();
                } catch (IOException e) {
                    Log.w(LOG_TAG, "Failed to close inputChannel.");
                }
            }
            if (outputChannel != null) {
                try {
                    outputChannel.close();
                } catch(IOException e) {
                    Log.w(LOG_TAG, "Failed to close outputChannel");
                }
            }
        }
        return destUri;
    }

    @Override
    public void setUp() {
        mImportProcessor = new ImportProcessor(getContext());
        mImportProcessor.ensureInit();
        mCopiedFileName = null;
    }

    @Override
    public void tearDown() {
        if (!TextUtils.isEmpty(mCopiedFileName)) {
            getContext().deleteFile(mCopiedFileName);
            mCopiedFileName = null;
        }
    }

    /**
     * Confirms {@link ImportProcessor#readOneVCard(android.net.Uri, int, String,
     * com.android.vcard.VCardInterpreter, int[])} successfully handles correct input.
     */
    public void testProcessSimple() throws IOException {
        final Uri uri = copyToLocal("v21_simple.vcf");
        final int vcardType = VCardSourceDetector.PARSE_TYPE_UNKNOWN;
        final String charset = null;
        final VCardInterpreter interpreter = new EmptyVCardInterpreter();
        final int[] versions = new int[] {
                ImportVCardActivity.VCARD_VERSION_V21
        };

        assertTrue(mImportProcessor.readOneVCard(
                uri, vcardType, charset, interpreter, versions));
    }

    /**
     * Confirms {@link ImportProcessor#handleOneRequest(ImportRequest)} accepts
     * one request and import it.
     */
    public void testHandleOneRequestSimple() throws IOException {
        CommitterGenerator generator = new CommitterGenerator() {
            public VCardEntryCommitter generate(ContentResolver resolver) {
                return new MockVCardEntryCommitter();
            }
        };
        mImportProcessor.injectCommitterGeneratorForTest(generator);
        mImportProcessor.initNotifierForTest();

        final ImportRequest request = new ImportRequest(
                null,  // account
                copyToLocal("v30_simple.vcf"),
                VCardSourceDetector.PARSE_TYPE_UNKNOWN,
                null,  // estimatedCharset
                ImportVCardActivity.VCARD_VERSION_AUTO_DETECT,
                1);
        assertTrue(mImportProcessor.handleOneRequest(request));
        assertEquals(1, mImportProcessor.getCreatedUrisForTest().size());
    }
}

/* package */ class EmptyVCardInterpreter implements VCardInterpreter {
    @Override
    public void end() {
    }
    @Override
    public void endEntry() {
    }
    @Override
    public void endProperty() {
    }
    @Override
    public void propertyGroup(String group) {
    }
    @Override
    public void propertyName(String name) {
    }
    @Override
    public void propertyParamType(String type) {
    }
    @Override
    public void propertyParamValue(String value) {
    }
    @Override
    public void propertyValues(List<String> values) {
    }
    @Override
    public void start() {
    }
    @Override
    public void startEntry() {
    }
    @Override
    public void startProperty() {
    }
}