/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.unit_tests.vcard;

import com.android.unit_tests.R;

import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentValues;
import android.net.Uri;
import android.pim.vcard.EntryCommitter;
import android.pim.vcard.VCardConfig;
import android.pim.vcard.VCardDataBuilder;
import android.pim.vcard.VCardParser;
import android.pim.vcard.VCardParser_V21;
import android.pim.vcard.VCardParser_V30;
import android.pim.vcard.exception.VCardException;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Event;
import android.provider.ContactsContract.CommonDataKinds.GroupMembership;
import android.provider.ContactsContract.CommonDataKinds.Im;
import android.provider.ContactsContract.CommonDataKinds.Nickname;
import android.provider.ContactsContract.CommonDataKinds.Note;
import android.provider.ContactsContract.CommonDataKinds.Organization;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.Photo;
import android.provider.ContactsContract.CommonDataKinds.Relation;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal;
import android.provider.ContactsContract.CommonDataKinds.Website;
import android.test.AndroidTestCase;
import android.test.mock.MockContentProvider;
import android.test.mock.MockContentResolver;
import android.text.TextUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map.Entry;

public class VCardTests extends AndroidTestCase {
    // Push data into int array at first since values like 0x80 are
    // interpreted as int by the compiler and casting all of them is
    // cumbersome...
    private static final int[] sPhotoIntArrayForComplicatedCase = {
        0xff, 0xd8, 0xff, 0xe1, 0x0a, 0x0f, 0x45, 0x78, 0x69, 0x66, 0x00,
        0x00, 0x4d, 0x4d, 0x00, 0x2a, 0x00, 0x00, 0x00, 0x08, 0x00, 0x0d,
        0x01, 0x0e, 0x00, 0x02, 0x00, 0x00, 0x00, 0x0f, 0x00, 0x00, 0x00,
        0xaa, 0x01, 0x0f, 0x00, 0x02, 0x00, 0x00, 0x00, 0x07, 0x00, 0x00,
        0x00, 0xba, 0x01, 0x10, 0x00, 0x02, 0x00, 0x00, 0x00, 0x06, 0x00,
        0x00, 0x00, 0xc2, 0x01, 0x12, 0x00, 0x03, 0x00, 0x00, 0x00, 0x01,
        0x00, 0x01, 0x00, 0x00, 0x01, 0x1a, 0x00, 0x05, 0x00, 0x00, 0x00,
        0x01, 0x00, 0x00, 0x00, 0xc8, 0x01, 0x1b, 0x00, 0x05, 0x00, 0x00,
        0x00, 0x01, 0x00, 0x00, 0x00, 0xd0, 0x01, 0x28, 0x00, 0x03, 0x00,
        0x00, 0x00, 0x01, 0x00, 0x02, 0x00, 0x00, 0x01, 0x31, 0x00, 0x02,
        0x00, 0x00, 0x00, 0x0e, 0x00, 0x00, 0x00, 0xd8, 0x01, 0x32, 0x00,
        0x02, 0x00, 0x00, 0x00, 0x14, 0x00, 0x00, 0x00, 0xe6, 0x02, 0x13,
        0x00, 0x03, 0x00, 0x00, 0x00, 0x01, 0x00, 0x01, 0x00, 0x00, 0x82,
        0x98, 0x00, 0x02, 0x00, 0x00, 0x00, 0x0e, 0x00, 0x00, 0x00, 0xfa,
        0x87, 0x69, 0x00, 0x04, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x01,
        0x84, 0xc4, 0xa5, 0x00, 0x07, 0x00, 0x00, 0x00, 0x7c, 0x00, 0x00,
        0x01, 0x08, 0x00, 0x00, 0x04, 0x1e, 0x32, 0x30, 0x30, 0x38, 0x31,
        0x30, 0x32, 0x39, 0x31, 0x33, 0x35, 0x35, 0x33, 0x31, 0x00, 0x00,
        0x44, 0x6f, 0x43, 0x6f, 0x4d, 0x6f, 0x00, 0x00, 0x44, 0x39, 0x30,
        0x35, 0x69, 0x00, 0x00, 0x00, 0x00, 0x48, 0x00, 0x00, 0x00, 0x01,
        0x00, 0x00, 0x00, 0x48, 0x00, 0x00, 0x00, 0x01, 0x44, 0x39, 0x30,
        0x35, 0x69, 0x20, 0x56, 0x65, 0x72, 0x31, 0x2e, 0x30, 0x30, 0x00,
        0x32, 0x30, 0x30, 0x38, 0x3a, 0x31, 0x30, 0x3a, 0x32, 0x39, 0x20,
        0x31, 0x33, 0x3a, 0x35, 0x35, 0x3a, 0x34, 0x37, 0x00, 0x20, 0x20,
        0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20,
        0x00, 0x50, 0x72, 0x69, 0x6e, 0x74, 0x49, 0x4d, 0x00, 0x30, 0x33,
        0x30, 0x30, 0x00, 0x00, 0x00, 0x06, 0x00, 0x01, 0x00, 0x14, 0x00,
        0x14, 0x00, 0x02, 0x01, 0x00, 0x00, 0x00, 0x00, 0x03, 0x00, 0x00,
        0x00, 0x34, 0x01, 0x00, 0x05, 0x00, 0x00, 0x00, 0x01, 0x01, 0x01,
        0x00, 0x00, 0x00, 0x01, 0x10, 0x80, 0x00, 0x00, 0x00, 0x00, 0x00,
        0x11, 0x09, 0x00, 0x00, 0x27, 0x10, 0x00, 0x00, 0x0f, 0x0b, 0x00,
        0x00, 0x27, 0x10, 0x00, 0x00, 0x05, 0x97, 0x00, 0x00, 0x27, 0x10,
        0x00, 0x00, 0x08, 0xb0, 0x00, 0x00, 0x27, 0x10, 0x00, 0x00, 0x1c,
        0x01, 0x00, 0x00, 0x27, 0x10, 0x00, 0x00, 0x02, 0x5e, 0x00, 0x00,
        0x27, 0x10, 0x00, 0x00, 0x00, 0x8b, 0x00, 0x00, 0x27, 0x10, 0x00,
        0x00, 0x03, 0xcb, 0x00, 0x00, 0x27, 0x10, 0x00, 0x00, 0x1b, 0xe5,
        0x00, 0x00, 0x27, 0x10, 0x00, 0x28, 0x82, 0x9a, 0x00, 0x05, 0x00,
        0x00, 0x00, 0x01, 0x00, 0x00, 0x03, 0x6a, 0x82, 0x9d, 0x00, 0x05,
        0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x03, 0x72, 0x88, 0x22, 0x00,
        0x03, 0x00, 0x00, 0x00, 0x01, 0x00, 0x02, 0x00, 0x00, 0x90, 0x00,
        0x00, 0x07, 0x00, 0x00, 0x00, 0x04, 0x30, 0x32, 0x32, 0x30, 0x90,
        0x03, 0x00, 0x02, 0x00, 0x00, 0x00, 0x14, 0x00, 0x00, 0x03, 0x7a,
        0x90, 0x04, 0x00, 0x02, 0x00, 0x00, 0x00, 0x14, 0x00, 0x00, 0x03,
        0x8e, 0x91, 0x01, 0x00, 0x07, 0x00, 0x00, 0x00, 0x04, 0x01, 0x02,
        0x03, 0x00, 0x91, 0x02, 0x00, 0x05, 0x00, 0x00, 0x00, 0x01, 0x00,
        0x00, 0x03, 0xa2, 0x92, 0x01, 0x00, 0x0a, 0x00, 0x00, 0x00, 0x01,
        0x00, 0x00, 0x03, 0xaa, 0x92, 0x02, 0x00, 0x05, 0x00, 0x00, 0x00,
        0x01, 0x00, 0x00, 0x03, 0xb2, 0x92, 0x04, 0x00, 0x0a, 0x00, 0x00,
        0x00, 0x01, 0x00, 0x00, 0x03, 0xba, 0x92, 0x05, 0x00, 0x05, 0x00,
        0x00, 0x00, 0x01, 0x00, 0x00, 0x03, 0xc2, 0x92, 0x07, 0x00, 0x03,
        0x00, 0x00, 0x00, 0x01, 0x00, 0x02, 0x00, 0x00, 0x92, 0x08, 0x00,
        0x03, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x92, 0x09,
        0x00, 0x03, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x92,
        0x0a, 0x00, 0x05, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x03, 0xca,
        0x92, 0x7c, 0x00, 0x07, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00,
        0x00, 0x92, 0x86, 0x00, 0x07, 0x00, 0x00, 0x00, 0x16, 0x00, 0x00,
        0x03, 0xd2, 0xa0, 0x00, 0x00, 0x07, 0x00, 0x00, 0x00, 0x04, 0x30,
        0x31, 0x30, 0x30, 0xa0, 0x01, 0x00, 0x03, 0x00, 0x00, 0x00, 0x01,
        0x00, 0x01, 0x00, 0x00, 0xa0, 0x02, 0x00, 0x03, 0x00, 0x00, 0x00,
        0x01, 0x00, 0x60, 0x00, 0x00, 0xa0, 0x03, 0x00, 0x03, 0x00, 0x00,
        0x00, 0x01, 0x00, 0x48, 0x00, 0x00, 0xa0, 0x05, 0x00, 0x04, 0x00,
        0x00, 0x00, 0x01, 0x00, 0x00, 0x04, 0x00, 0xa2, 0x0e, 0x00, 0x05,
        0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x03, 0xe8, 0xa2, 0x0f, 0x00,
        0x05, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x03, 0xf0, 0xa2, 0x10,
        0x00, 0x03, 0x00, 0x00, 0x00, 0x01, 0x00, 0x02, 0x00, 0x00, 0xa2,
        0x17, 0x00, 0x03, 0x00, 0x00, 0x00, 0x01, 0x00, 0x02, 0x00, 0x00,
        0xa3, 0x00, 0x00, 0x07, 0x00, 0x00, 0x00, 0x01, 0x03, 0x00, 0x00,
        0x00, 0xa3, 0x01, 0x00, 0x07, 0x00, 0x00, 0x00, 0x01, 0x01, 0x00,
        0x00, 0x00, 0xa4, 0x01, 0x00, 0x03, 0x00, 0x00, 0x00, 0x01, 0x00,
        0x00, 0x00, 0x00, 0xa4, 0x02, 0x00, 0x03, 0x00, 0x00, 0x00, 0x01,
        0x00, 0x00, 0x00, 0x00, 0xa4, 0x03, 0x00, 0x03, 0x00, 0x00, 0x00,
        0x01, 0x00, 0x00, 0x00, 0x00, 0xa4, 0x04, 0x00, 0x05, 0x00, 0x00,
        0x00, 0x01, 0x00, 0x00, 0x03, 0xf8, 0xa4, 0x05, 0x00, 0x03, 0x00,
        0x00, 0x00, 0x01, 0x00, 0x1d, 0x00, 0x00, 0xa4, 0x06, 0x00, 0x03,
        0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0xa4, 0x07, 0x00,
        0x03, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0xa4, 0x08,
        0x00, 0x03, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0xa4,
        0x09, 0x00, 0x03, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00,
        0xa4, 0x0a, 0x00, 0x03, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00,
        0x00, 0xa4, 0x0c, 0x00, 0x03, 0x00, 0x00, 0x00, 0x01, 0x00, 0x02,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x53, 0x00,
        0x00, 0x27, 0x10, 0x00, 0x00, 0x01, 0x5e, 0x00, 0x00, 0x00, 0x64,
        0x32, 0x30, 0x30, 0x38, 0x3a, 0x31, 0x30, 0x3a, 0x32, 0x39, 0x20,
        0x31, 0x33, 0x3a, 0x35, 0x35, 0x3a, 0x33, 0x31, 0x00, 0x32, 0x30,
        0x30, 0x38, 0x3a, 0x31, 0x30, 0x3a, 0x32, 0x39, 0x20, 0x31, 0x33,
        0x3a, 0x35, 0x35, 0x3a, 0x34, 0x37, 0x00, 0x00, 0x00, 0x29, 0x88,
        0x00, 0x00, 0x1b, 0x00, 0x00, 0x00, 0x02, 0xb2, 0x00, 0x00, 0x00,
        0x64, 0x00, 0x00, 0x01, 0x5e, 0x00, 0x00, 0x00, 0x64, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x64, 0x00, 0x00, 0x00, 0x25, 0x00,
        0x00, 0x00, 0x0a, 0x00, 0x00, 0x0e, 0x92, 0x00, 0x00, 0x03, 0xe8,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x32, 0x30, 0x30,
        0x38, 0x31, 0x30, 0x32, 0x39, 0x31, 0x33, 0x35, 0x35, 0x33, 0x31,
        0x00, 0x00, 0x20, 0x2a, 0x00, 0x00, 0x00, 0x0a, 0x00, 0x00, 0x2a,
        0xe2, 0x00, 0x00, 0x00, 0x0a, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        0x00, 0x01, 0x00, 0x02, 0x00, 0x01, 0x00, 0x02, 0x00, 0x00, 0x00,
        0x04, 0x52, 0x39, 0x38, 0x00, 0x00, 0x02, 0x00, 0x07, 0x00, 0x00,
        0x00, 0x04, 0x30, 0x31, 0x30, 0x30, 0x00, 0x00, 0x00, 0x00, 0x00,
        0x06, 0x01, 0x03, 0x00, 0x03, 0x00, 0x00, 0x00, 0x01, 0x00, 0x06,
        0x00, 0x00, 0x01, 0x1a, 0x00, 0x05, 0x00, 0x00, 0x00, 0x01, 0x00,
        0x00, 0x04, 0x6c, 0x01, 0x1b, 0x00, 0x05, 0x00, 0x00, 0x00, 0x01,
        0x00, 0x00, 0x04, 0x74, 0x01, 0x28, 0x00, 0x03, 0x00, 0x00, 0x00,
        0x01, 0x00, 0x02, 0x00, 0x00, 0x02, 0x01, 0x00, 0x04, 0x00, 0x00,
        0x00, 0x01, 0x00, 0x00, 0x04, 0x7c, 0x02, 0x02, 0x00, 0x04, 0x00,
        0x00, 0x00, 0x01, 0x00, 0x00, 0x05, 0x8b, 0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x48, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00,
        0x48, 0x00, 0x00, 0x00, 0x01, 0xff, 0xd8, 0xff, 0xdb, 0x00, 0x84,
        0x00, 0x20, 0x16, 0x18, 0x1c, 0x18, 0x14, 0x20, 0x1c, 0x1a, 0x1c,
        0x24, 0x22, 0x20, 0x26, 0x30, 0x50, 0x34, 0x30, 0x2c, 0x2c, 0x30,
        0x62, 0x46, 0x4a, 0x3a, 0x50, 0x74, 0x66, 0x7a, 0x78, 0x72, 0x66,
        0x70, 0x6e, 0x80, 0x90, 0xb8, 0x9c, 0x80, 0x88, 0xae, 0x8a, 0x6e,
        0x70, 0xa0, 0xda, 0xa2, 0xae, 0xbe, 0xc4, 0xce, 0xd0, 0xce, 0x7c,
        0x9a, 0xe2, 0xf2, 0xe0, 0xc8, 0xf0, 0xb8, 0xca, 0xce, 0xc6, 0x01,
        0x22, 0x24, 0x24, 0x30, 0x2a, 0x30, 0x5e, 0x34, 0x34, 0x5e, 0xc6,
        0x84, 0x70, 0x84, 0xc6, 0xc6, 0xc6, 0xc6, 0xc6, 0xc6, 0xc6, 0xc6,
        0xc6, 0xc6, 0xc6, 0xc6, 0xc6, 0xc6, 0xc6, 0xc6, 0xc6, 0xc6, 0xc6,
        0xc6, 0xc6, 0xc6, 0xc6, 0xc6, 0xc6, 0xc6, 0xc6, 0xc6, 0xc6, 0xc6,
        0xc6, 0xc6, 0xc6, 0xc6, 0xc6, 0xc6, 0xc6, 0xc6, 0xc6, 0xc6, 0xc6,
        0xc6, 0xc6, 0xc6, 0xc6, 0xc6, 0xc6, 0xc6, 0xc6, 0xc6, 0xff, 0xc0,
        0x00, 0x11, 0x08, 0x00, 0x78, 0x00, 0xa0, 0x03, 0x01, 0x21, 0x00,
        0x02, 0x11, 0x01, 0x03, 0x11, 0x01, 0xff, 0xc4, 0x01, 0xa2, 0x00,
        0x00, 0x01, 0x05, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x02, 0x03, 0x04, 0x05,
        0x06, 0x07, 0x08, 0x09, 0x0a, 0x0b, 0x10, 0x00, 0x02, 0x01, 0x03,
        0x03, 0x02, 0x04, 0x03, 0x05, 0x05, 0x04, 0x04, 0x00, 0x00, 0x01,
        0x7d, 0x01, 0x02, 0x03, 0x00, 0x04, 0x11, 0x05, 0x12, 0x21, 0x31,
        0x41, 0x06, 0x13, 0x51, 0x61, 0x07, 0x22, 0x71, 0x14, 0x32, 0x81,
        0x91, 0xa1, 0x08, 0x23, 0x42, 0xb1, 0xc1, 0x15, 0x52, 0xd1, 0xf0,
        0x24, 0x33, 0x62, 0x72, 0x82, 0x09, 0x0a, 0x16, 0x17, 0x18, 0x19,
        0x1a, 0x25, 0x26, 0x27, 0x28, 0x29, 0x2a, 0x34, 0x35, 0x36, 0x37,
        0x38, 0x39, 0x3a, 0x43, 0x44, 0x45, 0x46, 0x47, 0x48, 0x49, 0x4a,
        0x53, 0x54, 0x55, 0x56, 0x57, 0x58, 0x59, 0x5a, 0x63, 0x64, 0x65,
        0x66, 0x67, 0x68, 0x69, 0x6a, 0x73, 0x74, 0x75, 0x76, 0x77, 0x78,
        0x79, 0x7a, 0x83, 0x84, 0x85, 0x86, 0x87, 0x88, 0x89, 0x8a, 0x92,
        0x93, 0x94, 0x95, 0x96, 0x97, 0x98, 0x99, 0x9a, 0xa2, 0xa3, 0xa4,
        0xa5, 0xa6, 0xa7, 0xa8, 0xa9, 0xaa, 0xb2, 0xb3, 0xb4, 0xb5, 0xb6,
        0xb7, 0xb8, 0xb9, 0xba, 0xc2, 0xc3, 0xc4, 0xc5, 0xc6, 0xc7, 0xc8,
        0xc9, 0xca, 0xd2, 0xd3, 0xd4, 0xd5, 0xd6, 0xd7, 0xd8, 0xd9, 0xda,
        0xe1, 0xe2, 0xe3, 0xe4, 0xe5, 0xe6, 0xe7, 0xe8, 0xe9, 0xea, 0xf1,
        0xf2, 0xf3, 0xf4, 0xf5, 0xf6, 0xf7, 0xf8, 0xf9, 0xfa, 0x01, 0x00,
        0x03, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x00,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06,
        0x07, 0x08, 0x09, 0x0a, 0x0b, 0x11, 0x00, 0x02, 0x01, 0x02, 0x04,
        0x04, 0x03, 0x04, 0x07, 0x05, 0x04, 0x04, 0x00, 0x01, 0x02, 0x77,
        0x00, 0x01, 0x02, 0x03, 0x11, 0x04, 0x05, 0x21, 0x31, 0x06, 0x12,
        0x41, 0x51, 0x07, 0x61, 0x71, 0x13, 0x22, 0x32, 0x81, 0x08, 0x14,
        0x42, 0x91, 0xa1, 0xb1, 0xc1, 0x09, 0x23, 0x33, 0x52, 0xf0, 0x15,
        0x62, 0x72, 0xd1, 0x0a, 0x16, 0x24, 0x34, 0xe1, 0x25, 0xf1, 0x17,
        0x18, 0x19, 0x1a, 0x26, 0x27, 0x28, 0x29, 0x2a, 0x35, 0x36, 0x37,
        0x38, 0x39, 0x3a, 0x43, 0x44, 0x45, 0x46, 0x47, 0x48, 0x49, 0x4a,
        0x53, 0x54, 0x55, 0x56, 0x57, 0x58, 0x59, 0x5a, 0x63, 0x64, 0x65,
        0x66, 0x67, 0x68, 0x69, 0x6a, 0x73, 0x74, 0x75, 0x76, 0x77, 0x78,
        0x79, 0x7a, 0x82, 0x83, 0x84, 0x85, 0x86, 0x87, 0x88, 0x89, 0x8a,
        0x92, 0x93, 0x94, 0x95, 0x96, 0x97, 0x98, 0x99, 0x9a, 0xa2, 0xa3,
        0xa4, 0xa5, 0xa6, 0xa7, 0xa8, 0xa9, 0xaa, 0xb2, 0xb3, 0xb4, 0xb5,
        0xb6, 0xb7, 0xb8, 0xb9, 0xba, 0xc2, 0xc3, 0xc4, 0xc5, 0xc6, 0xc7,
        0xc8, 0xc9, 0xca, 0xd2, 0xd3, 0xd4, 0xd5, 0xd6, 0xd7, 0xd8, 0xd9,
        0xda, 0xe2, 0xe3, 0xe4, 0xe5, 0xe6, 0xe7, 0xe8, 0xe9, 0xea, 0xf2,
        0xf3, 0xf4, 0xf5, 0xf6, 0xf7, 0xf8, 0xf9, 0xfa, 0xff, 0xda, 0x00,
        0x0c, 0x03, 0x01, 0x00, 0x02, 0x11, 0x03, 0x11, 0x00, 0x3f, 0x00,
        0x14, 0x54, 0xaa, 0x2a, 0x46, 0x48, 0xa2, 0xa4, 0x55, 0xa6, 0x04,
        0x8a, 0x29, 0xe0, 0x53, 0x10, 0xe0, 0x29, 0xc0, 0x50, 0x03, 0xb1,
        0x46, 0x29, 0x80, 0x84, 0x52, 0x11, 0x40, 0x0d, 0x22, 0x9a, 0x45,
        0x20, 0x23, 0x61, 0x51, 0x30, 0xa0, 0x08, 0xc8, 0xa8, 0xd8, 0x52,
        0x02, 0x26, 0x15, 0x0b, 0x0a, 0x00, 0xb4, 0xa2, 0xa5, 0x5a, 0x00,
        0x91, 0x45, 0x4a, 0xa2, 0x81, 0x92, 0x01, 0x4e, 0x02, 0x98, 0x87,
        0x0a, 0x70, 0xa0, 0x07, 0x62, 0x8c, 0x50, 0x21, 0x0d, 0x25, 0x00,
        0x34, 0x8a, 0x61, 0x14, 0x0c, 0x63, 0x0a, 0x89, 0x85, 0x00, 0x46,
        0xd5, 0x1b, 0x52, 0x02, 0x16, 0xa8, 0x98, 0x50, 0x05, 0x94, 0xa9,
        0x16, 0x80, 0x25, 0x5a, 0x95, 0x68, 0x18, 0xf1, 0x4f, 0x14, 0xc4,
        0x3b, 0xb5, 0x22, 0xb6, 0x38, 0x34, 0x00, 0xe3, 0x22, 0x8e, 0xf4,
        0x79, 0x8a, 0x7b, 0xd1, 0x71, 0x03, 0x30, 0xc7, 0x14, 0x83, 0xa5,
        0x00, 0x06, 0x98, 0x68, 0x01, 0x8d, 0x51, 0x35, 0x03, 0x22, 0x6a,
        0x8d, 0xa9, 0x01, 0x13, 0x54, 0x4d, 0x40, 0x13, 0xa5, 0x4a, 0x28,
        0x02, 0x45, 0x35, 0x2a, 0x9a, 0x00, 0x78, 0x34, 0xf0, 0x69, 0x80,
        0x34, 0x81, 0x45, 0x40, 0xce, 0x58, 0xe6, 0xa2, 0x4c, 0x06, 0xe4,
        0xfa, 0xd1, 0x93, 0x50, 0x21, 0xca, 0xe4, 0x55, 0x84, 0x90, 0x30,
        0xab, 0x8b, 0x18, 0xa6, 0x9a, 0x6a, 0xc4, 0x31, 0xaa, 0x26, 0xa0,
        0x64, 0x4d, 0x51, 0xb5, 0x20, 0x23, 0x6a, 0x89, 0xa8, 0x02, 0x44,
        0x35, 0x2a, 0x9a, 0x00, 0x95, 0x4d, 0x48, 0xa6, 0x80, 0x24, 0x53,
        0x4e, 0xce, 0x05, 0x30, 0x2b, 0x3b, 0xee, 0x6a, 0x91, 0x5d, 0x76,
        0x63, 0xbd, 0x65, 0x7d, 0x40, 0x66, 0x68, 0xa9, 0x02, 0x45, 0x2b,
        0xb3, 0x9e, 0xb4, 0xc5, 0x6d, 0xad, 0x9a, 0xa0, 0x2c, 0x06, 0xc8,
        0xcd, 0x04, 0xd6, 0xa2, 0x23, 0x63, 0x51, 0xb1, 0xa0, 0x64, 0x4d,
        0x51, 0x93, 0x48, 0x08, 0xda, 0xa2, 0x6a, 0x00, 0x72, 0x1a, 0x99,
        0x4d, 0x00, 0x48, 0xa6, 0xa4, 0x53, 0x4c, 0x07, 0x86, 0x03, 0xbd,
        0x2b, 0x9c, 0xa7, 0x14, 0x98, 0x10, 0x85, 0x34, 0xe0, 0xa6, 0xb3,
        0xb0, 0x0b, 0xb5, 0xa8, 0x0a, 0xd4, 0x58, 0x42, 0xed, 0x3e, 0x94,
        0xd2, 0xa6, 0x8b, 0x01, 0x34, 0x44, 0xed, 0xe6, 0x9c, 0x4d, 0x6a,
        0x80, 0x8d, 0x8d, 0x46, 0xc6, 0x80, 0x23, 0x63, 0x51, 0x9a, 0x06,
        0x46, 0xd5, 0x13, 0x52, 0x01, 0x54, 0xd4, 0xaa, 0x68, 0x02, 0x40,
        0x6a, 0x40, 0x78, 0xa0, 0x08, 0x59, 0xce, 0xee, 0xb5, 0x2a, 0x39,
        0xd9, 0x59, 0xa7, 0xa8, 0x00, 0x73, 0xeb, 0x4e, 0x0e, 0x7d, 0x69,
        0x5c, 0x05, 0xf3, 0x0f, 0xad, 0x1e, 0x61, 0xf5, 0xa7, 0x71, 0x0b,
        0xe6, 0x35, 0x21, 0x90, 0xd3, 0xb8, 0x0e, 0x32, 0x10, 0x95, 0x10,
        0x91, 0xb3, 0xd6, 0x9b, 0x60, 0x4b, 0x9c, 0x8a, 0x63, 0x1a, 0xb0,
        0x18, 0x4d, 0x46, 0xc6, 0x80, 0x22, 0x6a, 0x61, 0xa4, 0x31, 0xaa,
        0x6a, 0x55, 0x34, 0x01, 0x2a, 0x9a, 0x7e, 0x78, 0xa0, 0x08, 0x09,
        0xf9, 0xaa, 0x58, 0xcf, 0xca, 0x6b, 0x3e, 0xa0, 0x00, 0xd3, 0x81,
        0xa9, 0x01, 0x73, 0x46, 0x69, 0x80, 0xb9, 0xa4, 0xcd, 0x00, 0x2b,
        0x1f, 0x92, 0xa3, 0x07, 0x9a, 0x6f, 0x70, 0x26, 0xcf, 0x14, 0xd2,
        0x6b, 0x51, 0x0c, 0x63, 0x51, 0xb1, 0xa0, 0x08, 0xda, 0x98, 0x69,
        0x0c, 0x8d, 0x4d, 0x4a, 0xa6, 0x80, 0x24, 0x53, 0x52, 0x03, 0xc5,
        0x02, 0x21, 0x27, 0xe6, 0xa9, 0x23, 0x3f, 0x29, 0xac, 0xfa, 0x8c,
        0x01, 0xe6, 0x9c, 0x0d, 0x48, 0x0a, 0x0d, 0x2e, 0x68, 0x01, 0x73,
        0x49, 0x9a, 0x60, 0x2b, 0x1f, 0x92, 0x98, 0x3a, 0xd3, 0x7b, 0x81,
        0x36, 0x78, 0xa6, 0x93, 0x5a, 0x88, 0x8c, 0x9a, 0x63, 0x1a, 0x00,
        0x8c, 0xd3, 0x0d, 0x21, 0x91, 0x29, 0xa9, 0x14, 0xd0, 0x04, 0x8a,
        0x69, 0xe0, 0xd3, 0x11, 0x1b, 0x1e, 0x6a, 0x48, 0xcf, 0xca, 0x6b,
        0x3e, 0xa3, 0x10, 0x1a, 0x70, 0x35, 0x20, 0x38, 0x1a, 0x5c, 0xd2,
        0x01, 0x73, 0x49, 0x9a, 0x60, 0x39, 0x8f, 0xca, 0x29, 0x8b, 0xf7,
        0xaa, 0xba, 0x88, 0x96, 0x9a, 0x6b, 0x40, 0x18, 0xc6, 0xa3, 0x26,
        0x80, 0x18, 0x69, 0xa6, 0x90, 0xc8, 0x14, 0xd4, 0x8a, 0x69, 0x80,
        0xf0, 0x6a, 0x40, 0x68, 0x10, 0xbb, 0x41, 0xa7, 0xe3, 0x0b, 0xc5,
        0x2b, 0x01, 0x10, 0xa7, 0x03, 0x59, 0x0c, 0x76, 0x69, 0x73, 0x40,
        0x0b, 0x9a, 0x28, 0x11, 0x28, 0x19, 0x5e, 0x69, 0x02, 0x81, 0x5a,
        0xd8, 0x00, 0xd3, 0x4d, 0x50, 0x0c, 0x6a, 0x8c, 0xd2, 0x01, 0xa6,
        0x98, 0x69, 0x0c, 0xae, 0xa6, 0xa4, 0x06, 0x80, 0x1e, 0xa6, 0x9e,
        0x0d, 0x31, 0x12, 0x03, 0x4f, 0x06, 0x80, 0x13, 0x60, 0x34, 0xd3,
        0xc1, 0xa8, 0x92, 0x01, 0xf1, 0x8d, 0xdd, 0x69, 0xcc, 0xa1, 0x69,
        0x5b, 0x4b, 0x80, 0x83, 0x93, 0x52, 0x04, 0x14, 0xe2, 0xae, 0x03,
        0xa9, 0x0d, 0x68, 0x03, 0x4d, 0x34, 0xd0, 0x03, 0x0d, 0x30, 0xd2,
        0x01, 0x86, 0x9a, 0x68, 0x19, 0x58, 0x1a, 0x78, 0xa4, 0x04, 0x8a,
        0x69, 0xe0, 0xd3, 0x10, 0xe0, 0x69, 0xe0, 0xd0, 0x03, 0xc1, 0xa8,
        0xdb, 0xad, 0x4c, 0x81, 0x12, 0x45, 0xd6, 0x9d, 0x25, 0x1d, 0x00,
        0x6a, 0xf5, 0xa9, 0xe8, 0x80, 0x31, 0x29, 0x0d, 0x58, 0x08, 0x69,
        0x86, 0x80, 0x1a, 0x69, 0x86, 0x90, 0x0c, 0x34, 0xd3, 0x48, 0x65,
        0x51, 0x4f, 0x06, 0x98, 0x0f, 0x14, 0xf0, 0x68, 0x10, 0xf0, 0x69,
        0xe0, 0xd0, 0x03, 0x81, 0xa5, 0x2b, 0x9a, 0x1a, 0xb8, 0x87, 0xa8,
        0xdb, 0x4a, 0x46, 0x68, 0xb6, 0x80, 0x2a, 0xa8, 0x14, 0xea, 0x12,
        0xb0, 0x05, 0x21, 0xa6, 0x02, 0x1a, 0x61, 0xa0, 0x06, 0x9a, 0x61,
        0xa4, 0x31, 0x86, 0x9a, 0x69, 0x0c, 0xa8, 0x0d, 0x3c, 0x53, 0x01,
        0xe2, 0x9e, 0x28, 0x10, 0xf1, 0x4e, 0x06, 0x98, 0x0f, 0x06, 0x9e,
        0x0d, 0x02, 0x1c, 0x29, 0xc2, 0x80, 0x16, 0x96, 0x80, 0x0a, 0x4a,
        0x00, 0x43, 0x4d, 0x34, 0x0c, 0x61, 0xa6, 0x1a, 0x40, 0x34, 0xd3,
        0x4d, 0x21, 0x80, 0xff, 0xd9, 0xff, 0xdb, 0x00, 0x84, 0x00, 0x0a,
        0x07, 0x07, 0x08, 0x07, 0x06, 0x0a, 0x08, 0x08, 0x08, 0x0b, 0x0a,
        0x0a, 0x0b, 0x0e, 0x18, 0x10, 0x0e, 0x0d, 0x0d, 0x0e, 0x1d, 0x15,
        0x16, 0x11, 0x18, 0x23, 0x1f, 0x25, 0x24, 0x22, 0x1f, 0x22, 0x21,
        0x26, 0x2b, 0x37, 0x2f, 0x26, 0x29, 0x34, 0x29, 0x21, 0x22, 0x30,
        0x41, 0x31, 0x34, 0x39, 0x3b, 0x3e, 0x3e, 0x3e, 0x25, 0x2e, 0x44,
        0x49, 0x43, 0x3c, 0x48, 0x37, 0x3d, 0x3e, 0x3b, 0x01, 0x0a, 0x0b,
        0x0b, 0x0e, 0x0d, 0x0e, 0x1c, 0x10, 0x10, 0x1c, 0x3b, 0x28, 0x22,
        0x28, 0x3b, 0x3b, 0x3b, 0x3b, 0x3b, 0x3b, 0x3b, 0x3b, 0x3b, 0x3b,
        0x3b, 0x3b, 0x3b, 0x3b, 0x3b, 0x3b, 0x3b, 0x3b, 0x3b, 0x3b, 0x3b,
        0x3b, 0x3b, 0x3b, 0x3b, 0x3b, 0x3b, 0x3b, 0x3b, 0x3b, 0x3b, 0x3b,
        0x3b, 0x3b, 0x3b, 0x3b, 0x3b, 0x3b, 0x3b, 0x3b, 0x3b, 0x3b, 0x3b,
        0x3b, 0x3b, 0x3b, 0x3b, 0x3b, 0x3b, 0x3b, 0xff, 0xc0, 0x00, 0x11,
        0x08, 0x00, 0x48, 0x00, 0x60, 0x03, 0x01, 0x21, 0x00, 0x02, 0x11,
        0x01, 0x03, 0x11, 0x01, 0xff, 0xc4, 0x01, 0xa2, 0x00, 0x00, 0x01,
        0x05, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07,
        0x08, 0x09, 0x0a, 0x0b, 0x10, 0x00, 0x02, 0x01, 0x03, 0x03, 0x02,
        0x04, 0x03, 0x05, 0x05, 0x04, 0x04, 0x00, 0x00, 0x01, 0x7d, 0x01,
        0x02, 0x03, 0x00, 0x04, 0x11, 0x05, 0x12, 0x21, 0x31, 0x41, 0x06,
        0x13, 0x51, 0x61, 0x07, 0x22, 0x71, 0x14, 0x32, 0x81, 0x91, 0xa1,
        0x08, 0x23, 0x42, 0xb1, 0xc1, 0x15, 0x52, 0xd1, 0xf0, 0x24, 0x33,
        0x62, 0x72, 0x82, 0x09, 0x0a, 0x16, 0x17, 0x18, 0x19, 0x1a, 0x25,
        0x26, 0x27, 0x28, 0x29, 0x2a, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39,
        0x3a, 0x43, 0x44, 0x45, 0x46, 0x47, 0x48, 0x49, 0x4a, 0x53, 0x54,
        0x55, 0x56, 0x57, 0x58, 0x59, 0x5a, 0x63, 0x64, 0x65, 0x66, 0x67,
        0x68, 0x69, 0x6a, 0x73, 0x74, 0x75, 0x76, 0x77, 0x78, 0x79, 0x7a,
        0x83, 0x84, 0x85, 0x86, 0x87, 0x88, 0x89, 0x8a, 0x92, 0x93, 0x94,
        0x95, 0x96, 0x97, 0x98, 0x99, 0x9a, 0xa2, 0xa3, 0xa4, 0xa5, 0xa6,
        0xa7, 0xa8, 0xa9, 0xaa, 0xb2, 0xb3, 0xb4, 0xb5, 0xb6, 0xb7, 0xb8,
        0xb9, 0xba, 0xc2, 0xc3, 0xc4, 0xc5, 0xc6, 0xc7, 0xc8, 0xc9, 0xca,
        0xd2, 0xd3, 0xd4, 0xd5, 0xd6, 0xd7, 0xd8, 0xd9, 0xda, 0xe1, 0xe2,
        0xe3, 0xe4, 0xe5, 0xe6, 0xe7, 0xe8, 0xe9, 0xea, 0xf1, 0xf2, 0xf3,
        0xf4, 0xf5, 0xf6, 0xf7, 0xf8, 0xf9, 0xfa, 0x01, 0x00, 0x03, 0x01,
        0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08,
        0x09, 0x0a, 0x0b, 0x11, 0x00, 0x02, 0x01, 0x02, 0x04, 0x04, 0x03,
        0x04, 0x07, 0x05, 0x04, 0x04, 0x00, 0x01, 0x02, 0x77, 0x00, 0x01,
        0x02, 0x03, 0x11, 0x04, 0x05, 0x21, 0x31, 0x06, 0x12, 0x41, 0x51,
        0x07, 0x61, 0x71, 0x13, 0x22, 0x32, 0x81, 0x08, 0x14, 0x42, 0x91,
        0xa1, 0xb1, 0xc1, 0x09, 0x23, 0x33, 0x52, 0xf0, 0x15, 0x62, 0x72,
        0xd1, 0x0a, 0x16, 0x24, 0x34, 0xe1, 0x25, 0xf1, 0x17, 0x18, 0x19,
        0x1a, 0x26, 0x27, 0x28, 0x29, 0x2a, 0x35, 0x36, 0x37, 0x38, 0x39,
        0x3a, 0x43, 0x44, 0x45, 0x46, 0x47, 0x48, 0x49, 0x4a, 0x53, 0x54,
        0x55, 0x56, 0x57, 0x58, 0x59, 0x5a, 0x63, 0x64, 0x65, 0x66, 0x67,
        0x68, 0x69, 0x6a, 0x73, 0x74, 0x75, 0x76, 0x77, 0x78, 0x79, 0x7a,
        0x82, 0x83, 0x84, 0x85, 0x86, 0x87, 0x88, 0x89, 0x8a, 0x92, 0x93,
        0x94, 0x95, 0x96, 0x97, 0x98, 0x99, 0x9a, 0xa2, 0xa3, 0xa4, 0xa5,
        0xa6, 0xa7, 0xa8, 0xa9, 0xaa, 0xb2, 0xb3, 0xb4, 0xb5, 0xb6, 0xb7,
        0xb8, 0xb9, 0xba, 0xc2, 0xc3, 0xc4, 0xc5, 0xc6, 0xc7, 0xc8, 0xc9,
        0xca, 0xd2, 0xd3, 0xd4, 0xd5, 0xd6, 0xd7, 0xd8, 0xd9, 0xda, 0xe2,
        0xe3, 0xe4, 0xe5, 0xe6, 0xe7, 0xe8, 0xe9, 0xea, 0xf2, 0xf3, 0xf4,
        0xf5, 0xf6, 0xf7, 0xf8, 0xf9, 0xfa, 0xff, 0xda, 0x00, 0x0c, 0x03,
        0x01, 0x00, 0x02, 0x11, 0x03, 0x11, 0x00, 0x3f, 0x00, 0x9e, 0xd2,
        0x2e, 0x07, 0x15, 0xaf, 0x6d, 0x08, 0xe2, 0xb3, 0x45, 0x1a, 0xf6,
        0xd0, 0x00, 0x01, 0xc5, 0x68, 0x45, 0x17, 0x4a, 0xb4, 0x22, 0xe4,
        0x70, 0x8c, 0x74, 0xa9, 0x3c, 0xa1, 0x8e, 0x95, 0x48, 0x96, 0x31,
        0xe2, 0x18, 0xe9, 0x55, 0xa5, 0x8c, 0x7a, 0x50, 0x05, 0x0b, 0x88,
        0x86, 0x0f, 0x15, 0x8f, 0x75, 0x1f, 0x26, 0x93, 0x19, 0x91, 0x77,
        0x18, 0xc1, 0xac, 0x4b, 0xc8, 0xfa, 0xd6, 0x63, 0x37, 0x6d, 0x31,
        0xb4, 0x73, 0x5b, 0x36, 0xa0, 0x1c, 0x50, 0x80, 0xd7, 0x83, 0xa0,
        0xab, 0xd1, 0x62, 0xad, 0x09, 0x8f, 0x17, 0x29, 0x03, 0xb2, 0xcc,
        0xe0, 0x77, 0x14, 0xa3, 0x56, 0xb3, 0x27, 0x1e, 0x67, 0xe9, 0x52,
        0xea, 0xc6, 0x3a, 0x36, 0x48, 0xef, 0x3d, 0x27, 0x70, 0x22, 0x60,
        0x47, 0x52, 0x69, 0xb2, 0xe2, 0xad, 0x3b, 0xea, 0x80, 0xa3, 0x38,
        0xe0, 0xd6, 0x3d, 0xd8, 0x1c, 0xd0, 0xca, 0x46, 0x3d, 0xd0, 0x18,
        0x35, 0x89, 0x78, 0xa3, 0x9a, 0xcd, 0x8c, 0xd2, 0xb3, 0x93, 0x2a,
        0x2b, 0x66, 0xd5, 0xf1, 0x8a, 0x10, 0x1a, 0xd6, 0xf2, 0x03, 0x8a,
        0x9e, 0xe6, 0xf4, 0x5a, 0xdb, 0xef, 0xfe, 0x23, 0xc0, 0xa7, 0x27,
        0xcb, 0x16, 0xc4, 0xcc, 0xdd, 0xe2, 0x78, 0x9a, 0x69, 0x66, 0xcc,
        0x99, 0xe1, 0x4d, 0x47, 0xba, 0xbc, 0xd9, 0x6a, 0xee, 0x26, 0x59,
        0x59, 0x4d, 0xac, 0x69, 0x34, 0x52, 0xe5, 0x8f, 0x55, 0xad, 0x58,
        0xae, 0x85, 0xc4, 0x22, 0x41, 0xdf, 0xad, 0x76, 0x61, 0xe5, 0x6f,
        0x74, 0x45, 0x69, 0xdc, 0x00, 0x79, 0xac, 0x8b, 0xa6, 0xc9, 0x35,
        0xd4, 0x34, 0x64, 0xdc, 0x37, 0x06, 0xb1, 0xae, 0x88, 0xc1, 0xac,
        0xd8, 0xc9, 0x2c, 0xa6, 0xe0, 0x73, 0x5b, 0x36, 0xf3, 0x74, 0xe6,
        0x84, 0x05, 0xe3, 0xa9, 0x47, 0x6a, 0x14, 0xb6, 0x49, 0x3d, 0x85,
        0x3a, 0xee, 0xee, 0x2b, 0xa8, 0xe2, 0x6f, 0x30, 0x81, 0xe9, 0x8a,
        0xca, 0xa4, 0xe2, 0xd3, 0x8b, 0x01, 0xb1, 0xf9, 0x04, 0x7f, 0xaf,
        0x23, 0xf0, 0xa9, 0x54, 0x41, 0x9c, 0xfd, 0xa3, 0xf4, 0xae, 0x65,
        0x18, 0xf7, 0x25, 0x8a, 0xe2, 0x02, 0x38, 0xb8, 0xfd, 0x2a, 0x7b,
        0x5b, 0xa8, 0x6d, 0x6d, 0x5d, 0x9a, 0x5d, 0xcb, 0xbb, 0xd2, 0xb6,
        0xa6, 0xa3, 0x19, 0x5e, 0xe2, 0x03, 0x7b, 0x1d, 0xc2, 0x17, 0x8d,
        0xb8, 0xac, 0xfb, 0x89, 0x39, 0x35, 0xd6, 0x9a, 0x6a, 0xe8, 0x66,
        0x55, 0xcb, 0xf5, 0xac, 0x7b, 0x96, 0xeb, 0x50, 0xc6, 0x88, 0x6d,
        0x66, 0xe9, 0xcd, 0x6c, 0xdb, 0x4f, 0xd3, 0x9a, 0x00, 0x2f, 0xe6,
        0xf9, 0xa3, 0xe7, 0xb5, 0x4a, 0x93, 0x7f, 0xa2, 0xc6, 0x73, 0xdc,
        0xd7, 0x15, 0x55, 0xef, 0x48, 0x7d, 0x09, 0x52, 0x6e, 0x3a, 0xd4,
        0xab, 0x2f, 0xbd, 0x61, 0x16, 0x0c, 0x73, 0x49, 0xc5, 0x24, 0x92,
        0x7f, 0xa2, 0x63, 0xfd, 0xaa, 0xd6, 0x2f, 0x71, 0x0e, 0xb1, 0x93,
        0xf7, 0x2d, 0xf5, 0xa4, 0x9e, 0x4e, 0xb5, 0xdd, 0x4b, 0xf8, 0x68,
        0x4c, 0xcb, 0xb9, 0x93, 0xad, 0x65, 0xce, 0xd9, 0x26, 0xa9, 0x8d,
        0x19, 0xf6, 0xf2, 0xf4, 0xe6, 0xb5, 0xad, 0xe7, 0xc6, 0x39, 0xa0,
        0x18, 0xeb, 0xc9, 0x77, 0x6c, 0x35, 0x2a, 0x4b, 0xfe, 0x8a, 0x9c,
        0xff, 0x00, 0x11, 0xae, 0x3a, 0x8b, 0xde, 0x61, 0xd0, 0x9e, 0x39,
        0xb8, 0xeb, 0x53, 0xac, 0xb9, 0xae, 0x5b, 0x00, 0xf3, 0x27, 0x14,
        0x92, 0xc9, 0xfe, 0x8a, 0x3f, 0xde, 0x35, 0xac, 0x3a, 0x88, 0x92,
        0xcd, 0xb1, 0x6e, 0x7d, 0xcd, 0x32, 0x67, 0xeb, 0xcd, 0x7a, 0x14,
        0xfe, 0x04, 0x26, 0x66, 0xce, 0xf9, 0x26, 0xb3, 0xe6, 0x6e, 0xb4,
        0xd9, 0x48, 0xc8, 0x82, 0x4e, 0x07, 0x35, 0xa7, 0x6f, 0x2f, 0x02,
        0x9a, 0x06, 0x5f, 0x8c, 0xa4, 0x83, 0x0e, 0x32, 0x2a, 0x69, 0xe3,
        0xdd, 0x12, 0x08, 0x97, 0x85, 0xec, 0x2a, 0x2a, 0x42, 0xf1, 0x76,
        0x26, 0xe4, 0x6a, 0x59, 0x0e, 0x18, 0x10, 0x6a, 0xd2, 0x89, 0x02,
        0x6e, 0x2a, 0x71, 0xeb, 0x5c, 0x1c, 0x8c, 0xa6, 0x48, 0xbb, 0xdc,
        0x61, 0x41, 0x35, 0x72, 0x28, 0x87, 0xd9, 0xf6, 0x4a, 0xb9, 0xe7,
        0x38, 0xae, 0x8c, 0x3d, 0x36, 0xdd, 0xde, 0xc4, 0xb0, 0x21, 0x51,
        0x76, 0xa8, 0xc0, 0xaa, 0x93, 0x31, 0xe6, 0xbb, 0x2d, 0x65, 0x61,
        0x19, 0xd3, 0x1e, 0xb5, 0x46, 0x5a, 0x96, 0x5a, 0x30, 0xa0, 0x7e,
        0x05, 0x69, 0x5b, 0xc9, 0xc6, 0x28, 0x40, 0xcd, 0x08, 0x64, 0x3c,
        0x73, 0x57, 0xe1, 0x94, 0xf1, 0xcd, 0x5a, 0x21, 0x8c, 0xb9, 0x63,
        0xe7, 0x67, 0x1d, 0xab, 0x40, 0xb1, 0xfb, 0x00, 0x1d, 0xf0, 0x2b,
        0x99, 0x2d, 0x66, 0x3e, 0x88, 0x75, 0x81, 0x3f, 0x31, 0xf6, 0xab,
        0x64, 0xd6, 0xb4, 0x17, 0xee, 0xd0, 0x9e, 0xe4, 0x32, 0x1a, 0xa7,
        0x31, 0xad, 0x18, 0x14, 0x26, 0xef, 0x54, 0xa5, 0xa8, 0x65, 0xa3,
        0x9c, 0x81, 0xfa, 0x56, 0x8c, 0x2d, 0xce, 0x68, 0x40, 0xcb, 0xf1,
        0x37, 0xbd, 0x5e, 0x85, 0xea, 0xd1, 0x0c, 0xbb, 0x19, 0x56, 0x23,
        0x20, 0x1f, 0xad, 0x5c, 0x42, 0x08, 0x03, 0xb5, 0x55, 0x91, 0x04,
        0xc9, 0x80, 0x38, 0x00, 0x0a, 0x71, 0x34, 0x6c, 0x32, 0x27, 0xe9,
        0x55, 0x25, 0x15, 0x2c, 0x68, 0xa3, 0x30, 0xeb, 0x54, 0xa5, 0x15,
        0x0c, 0xd1, 0x00, 0xff, 0xd9};

    private static final byte[] sPhotoByteArrayForComplicatedCase;

    static {
        final int length = sPhotoIntArrayForComplicatedCase.length;
        sPhotoByteArrayForComplicatedCase = new byte[length];
        for (int i = 0; i < length; i++) {
            sPhotoByteArrayForComplicatedCase[i] = (byte)sPhotoIntArrayForComplicatedCase[i];
        }
    }

    private class PropertyNodesVerifier {
        private HashMap<String, List<PropertyNode>> mPropertyNodeMap;
        public PropertyNodesVerifier() {
            mPropertyNodeMap = new HashMap<String, List<PropertyNode>>();
        }

        public PropertyNodesVerifier addPropertyNode(String propName, String propValue,
                List<String> propValue_vector, byte[] propValue_bytes,
                ContentValues paramMap, Set<String> paramMap_TYPE, Set<String> propGroupSet) {
            PropertyNode propertyNode = new PropertyNode(propName,
                    propValue, propValue_vector, propValue_bytes,
                    paramMap, paramMap_TYPE, propGroupSet);
            List<PropertyNode> expectedNodeList = mPropertyNodeMap.get(propName);
            if (expectedNodeList == null) {
                expectedNodeList = new ArrayList<PropertyNode>();
                mPropertyNodeMap.put(propName, expectedNodeList);
            }
            expectedNodeList.add(propertyNode);
            return this;
        }

        public void verify(VNode vnode) {
            for (PropertyNode propertyNode : vnode.propList) {
                String propName = propertyNode.propName;
                List<PropertyNode> nodes = mPropertyNodeMap.get(propName);
                if (nodes == null) {
                    fail("Unexpected propName \"" + propName + "\" exists.");
                }
                boolean successful = false;
                int size = nodes.size();
                for (int i = 0; i < size; i++) {
                    PropertyNode expectedNode = nodes.get(i);
                    if (expectedNode.propName.equals(propName)) {
                        if (expectedNode.equals(propertyNode)) {
                            successful = true;
                            nodes.remove(i);
                            if (nodes.size() == 0) {
                                mPropertyNodeMap.remove(propName);
                            }
                            break;
                        } else {
                            fail("Property \"" + propName + "\" has wrong value.\n" 
                                    + "expected: " + expectedNode.toString() 
                                    + "\n  actual: " + propertyNode.toString());
                        }
                    }
                }
                if (!successful) {
                    fail("Unexpected property \"" + propName + "\" exists.");
                }
            }
            if (mPropertyNodeMap.size() != 0) {
                List<String> expectedProps = new ArrayList<String>();
                for (List<PropertyNode> nodes : mPropertyNodeMap.values()) {
                    for (PropertyNode node : nodes) {
                        expectedProps.add(node.propName);
                    }
                }
                fail("expected props " + Arrays.toString(expectedProps.toArray()) +
                        " was not found");
            }
        }
    }

    public class VerificationResolver extends MockContentResolver {
        VerificationProvider mVerificationProvider = new VerificationProvider();
        @Override
        public ContentProviderResult[] applyBatch(String authority,
                ArrayList<ContentProviderOperation> operations) {
            equalsString(authority, RawContacts.CONTENT_URI.toString());
            return mVerificationProvider.applyBatch(operations);
        }

        public void addExpectedContentValues(ContentValues expectedContentValues) {
            mVerificationProvider.addExpectedContentValues(expectedContentValues);
        }

        public void verify() {
            mVerificationProvider.verify();
        }
    }

    private static final Set<String> sKnownMimeTypeSet =
        new HashSet<String>(Arrays.asList(StructuredName.CONTENT_ITEM_TYPE,
                Nickname.CONTENT_ITEM_TYPE, Phone.CONTENT_ITEM_TYPE,
                Email.CONTENT_ITEM_TYPE, StructuredPostal.CONTENT_ITEM_TYPE,
                Im.CONTENT_ITEM_TYPE, Organization.CONTENT_ITEM_TYPE,
                Event.CONTENT_ITEM_TYPE, Photo.CONTENT_ITEM_TYPE,
                Note.CONTENT_ITEM_TYPE, Website.CONTENT_ITEM_TYPE,
                Relation.CONTENT_ITEM_TYPE, Event.CONTENT_ITEM_TYPE,
                GroupMembership.CONTENT_ITEM_TYPE));

    private static boolean equalsForContentValues(
            ContentValues expected, ContentValues actual) {
        if (expected == actual) {
            return true;
        } else if (expected == null || actual == null || expected.size() != actual.size()) {
            return false;
        }
        for (Entry<String, Object> entry : expected.valueSet()) {
            final String key = entry.getKey();
            final Object value = entry.getValue();
            if (!actual.containsKey(key)) {
                return false;
            }
            if (value instanceof byte[]) {
                Object actualValue = actual.get(key);
                if (!Arrays.equals((byte[])value, (byte[])actualValue)) {
                    return false;
                }
            } else if (!value.equals(actual.get(key))) {
                    return false;
            }
        }
        return true;
    }

    class VerificationProvider extends MockContentProvider {
        final Map<String, Collection<ContentValues>> mMimeTypeToExpectedContentValues;

        public VerificationProvider() {
            mMimeTypeToExpectedContentValues =
                new HashMap<String, Collection<ContentValues>>();
            for (String acceptanbleMimeType : sKnownMimeTypeSet) {
                // Do not use HashSet since the current implementation changes the content of
                // ContentValues after the insertion, which make the result of hashCode()
                // changes...
                mMimeTypeToExpectedContentValues.put(
                        acceptanbleMimeType, new ArrayList<ContentValues>());
            }
        }

        public void addExpectedContentValues(ContentValues expectedContentValues) {
            final String mimeType = expectedContentValues.getAsString(Data.MIMETYPE);
            if (!sKnownMimeTypeSet.contains(mimeType)) {
                fail(String.format(
                        "Unknow MimeType %s in the test code. Test code should be broken.",
                        mimeType));
            }

            final Collection<ContentValues> contentValuesCollection =
                mMimeTypeToExpectedContentValues.get(mimeType);
            contentValuesCollection.add(expectedContentValues);
        }

        @Override
        public ContentProviderResult[] applyBatch(
                ArrayList<ContentProviderOperation> operations) {
            if (operations == null) {
                fail("There is no operation.");
            }

            final int size = operations.size();
            ContentProviderResult[] fakeResultArray = new ContentProviderResult[size];
            for (int i = 0; i < size; i++) {
                Uri uri = Uri.withAppendedPath(RawContacts.CONTENT_URI, String.valueOf(i));
                fakeResultArray[i] = new ContentProviderResult(uri);
            }

            for (int i = 0; i < size; i++) {
                ContentProviderOperation operation = operations.get(i);
                ContentValues actualContentValues = operation.resolveValueBackReferences(
                        fakeResultArray, i);
                final Uri uri = operation.getUri();
                if (uri.equals(RawContacts.CONTENT_URI)) {
                    assertNull(actualContentValues.get(RawContacts.ACCOUNT_NAME));
                    assertNull(actualContentValues.get(RawContacts.ACCOUNT_TYPE));
                } else if (uri.equals(Data.CONTENT_URI)) {
                    final String mimeType = actualContentValues.getAsString(Data.MIMETYPE);
                    if (!sKnownMimeTypeSet.contains(mimeType)) {
                        fail(String.format(
                                "Unknown MimeType %s. Probably added after developing this test",
                                mimeType));
                    }
                    // Remove data meaningless in this unit tests.
                    // Specifically, Data.DATA1 - DATA7 are set to null or empty String
                    // regardless of the input, but it may change depending on how
                    // resolver-related code handles it.
                    // Here, we ignore these implementation-dependent specs and
                    // just check whether vCard importer correctly inserts rellevent data.
                    Set<String> keyToBeRemoved = new HashSet<String>();
                    for (Entry<String, Object> entry : actualContentValues.valueSet()) {
                        Object value = entry.getValue();
                        if (value == null || TextUtils.isEmpty(value.toString())) {
                            keyToBeRemoved.add(entry.getKey());
                        }
                    }
                    for (String key: keyToBeRemoved) {
                        actualContentValues.remove(key);
                    }
                    /* For testing
                    Log.d("@@@",
                            String.format("MimeType: %s, data: %s",
                                    mimeType, actualContentValues.toString()));
                     */
                    // Remove RAW_CONTACT_ID entry just for safety, since we do not care
                    // how resolver-related code handles the entry in this unit test,
                    if (actualContentValues.containsKey(Data.RAW_CONTACT_ID)) {
                        actualContentValues.remove(Data.RAW_CONTACT_ID);
                    }
                    final Collection<ContentValues> contentValuesCollection =
                        mMimeTypeToExpectedContentValues.get(mimeType);
                    if (contentValuesCollection == null) {
                        fail("ContentValues for MimeType " + mimeType
                                + " is not expected at all (" + actualContentValues + ")");
                    }
                    boolean checked = false;
                    for (ContentValues expectedContentValues : contentValuesCollection) {
                        /* For testing
                        Log.d("@@@", "expected: "
                                + convertToEasilyReadableString(expectedContentValues));
                        Log.d("@@@", "actual  : "
                                + convertToEasilyReadableString(actualContentValues));
                         */
                        if (equalsForContentValues(expectedContentValues,
                                actualContentValues)) {
                            assertTrue(contentValuesCollection.remove(expectedContentValues));
                            checked = true;
                            break;
                        }
                    }
                    if (!checked) {
                        final String failMsg =
                            "Unexpected ContentValues for MimeType " + mimeType
                            + ": " + actualContentValues;
                        fail(failMsg);
                    }
                } else {
                    fail("Unexpected Uri has come: " + uri);
                }
            }  // for (int i = 0; i < size; i++) {
            return null;
        }

        public void verify() {
            StringBuilder builder = new StringBuilder();
            for (Collection<ContentValues> contentValuesCollection :
                    mMimeTypeToExpectedContentValues.values()) {
                for (ContentValues expectedContentValues: contentValuesCollection) {
                    builder.append(convertToEasilyReadableString(expectedContentValues));
                    builder.append("\n");
                }
            }
            if (builder.length() > 0) {
                final String failMsg = 
                    "There is(are) remaining expected ContentValues instance(s): \n"
                        + builder.toString();
                fail(failMsg);
            }
        }
    }

    /**
     * Utility method to print ContentValues whose content is printed with sorted keys.
     */
    private static String convertToEasilyReadableString(ContentValues contentValues) {
        if (contentValues == null) {
            return "null";
        }
        String mimeTypeValue = "";
        SortedMap<String, String> sortedMap = new TreeMap<String, String>();
        for (Entry<String, Object> entry : contentValues.valueSet()) {
            final String key = entry.getKey();
            final String value = entry.getValue().toString();
            if (Data.MIMETYPE.equals(key)) {
                mimeTypeValue = value;
            } else {
                assertNotNull(key);
                sortedMap.put(key, (value != null ? value.toString() : ""));
            }
        }
        StringBuilder builder = new StringBuilder();
        builder.append(Data.MIMETYPE);
        builder.append('=');
        builder.append(mimeTypeValue);
        for (Entry<String, String> entry : sortedMap.entrySet()) {
            final String key = entry.getKey();
            final String value = entry.getValue();
            builder.append(' ');
            builder.append(key);
            builder.append('=');
            builder.append(value);
        }
        return builder.toString();
    }
    
    private static boolean equalsString(String a, String b) {
        if (a == null || a.length() == 0) {
            return b == null || b.length() == 0;
        } else {
            return a.equals(b);
        }
    }

    private class ContactStructVerifier {
        private final int mResourceId;
        private final int mVCardType;
        private final VerificationResolver mResolver;
        // private final String mCharset;
        public ContactStructVerifier(int resId, int vCardType) {
            mResourceId = resId;
            mVCardType = vCardType;
            mResolver = new VerificationResolver();
        }
        
        public ContentValues createExpected(String mimeType) {
            ContentValues contentValues = new ContentValues();
            contentValues.put(Data.MIMETYPE, mimeType);
            mResolver.addExpectedContentValues(contentValues);
            return contentValues;
        }
        
        public void verify() throws IOException, VCardException {
            InputStream is = getContext().getResources().openRawResource(mResourceId);
            final VCardParser vCardParser;
            if (VCardConfig.isV30(mVCardType)) {
                vCardParser = new VCardParser_V30();
            } else {
                vCardParser = new VCardParser_V21();
            }
            VCardDataBuilder builder =
                new VCardDataBuilder(null, null, false, mVCardType, null);
            builder.addEntryHandler(new EntryCommitter(mResolver));
            try {
                vCardParser.parse(is, builder);
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                    }
                }
            }
            mResolver.verify();
        }
    }

    public void testV21SimpleCase1_Parsing() throws IOException, VCardException {
        VCardParser_V21 parser = new VCardParser_V21();
        VNodeBuilder builder = new VNodeBuilder();
        InputStream is = getContext().getResources().openRawResource(R.raw.v21_simple_1);
        assertEquals(true, parser.parse(is,"ISO-8859-1", builder));
        is.close();
        assertEquals(1, builder.vNodeList.size());
        PropertyNodesVerifier verifier = new PropertyNodesVerifier()
            .addPropertyNode("N", "Ando;Roid;", Arrays.asList("Ando", "Roid", ""),
                    null, null, null, null);
        verifier.verify(builder.vNodeList.get(0));
    }

    public void testV21SimpleCase1_Type_Generic() throws IOException, VCardException {
        ContactStructVerifier verifier = new ContactStructVerifier(
                R.raw.v21_simple_1, VCardConfig.VCARD_TYPE_V21_GENERIC);
        ContentValues contentValues =
            verifier.createExpected(StructuredName.CONTENT_ITEM_TYPE);
        contentValues.put(StructuredName.FAMILY_NAME, "Ando");
        contentValues.put(StructuredName.GIVEN_NAME, "Roid");
        contentValues.put(StructuredName.DISPLAY_NAME, "Roid Ando");
        verifier.verify();
    }

    public void testV21SimpleCase1_Type_Japanese() throws IOException, VCardException {
        ContactStructVerifier verifier = new ContactStructVerifier(
                R.raw.v21_simple_1, VCardConfig.VCARD_TYPE_V21_JAPANESE);
        ContentValues contentValues =
            verifier.createExpected(StructuredName.CONTENT_ITEM_TYPE);
        contentValues.put(StructuredName.FAMILY_NAME, "Ando");
        contentValues.put(StructuredName.GIVEN_NAME, "Roid");
        // If name-related strings only contains printable Ascii, the order is remained to be US's:
        // "Prefix Given Middle Family Suffix"
        contentValues.put(StructuredName.DISPLAY_NAME, "Roid Ando");
        verifier.verify();
    }

    public void testV21SimpleCase2() throws IOException, VCardException {
        ContactStructVerifier verifier = new ContactStructVerifier(
                R.raw.v21_simple_2, VCardConfig.VCARD_TYPE_V21_GENERIC);
        ContentValues contentValues =
            verifier.createExpected(StructuredName.CONTENT_ITEM_TYPE);
        contentValues.put(StructuredName.DISPLAY_NAME, "Ando Roid");
        verifier.verify();
    }

    public void testV21SimpleCase3() throws IOException, VCardException {
        ContactStructVerifier verifier = new ContactStructVerifier(
                R.raw.v21_simple_3, VCardConfig.VCARD_TYPE_V21_GENERIC);
        ContentValues contentValues =
            verifier.createExpected(StructuredName.CONTENT_ITEM_TYPE);
        contentValues.put(StructuredName.FAMILY_NAME, "Ando");
        contentValues.put(StructuredName.GIVEN_NAME, "Roid");
        // "FN" field should be prefered since it should contain the original order intended by
        // the author of the file.
        contentValues.put(StructuredName.DISPLAY_NAME, "Ando Roid");
        verifier.verify();
    }

    /**
     * Tests ';' is properly handled by VCardParser implementation.
     */
    public void testV21BackslashCase_Parsing() throws IOException, VCardException {
        VCardParser_V21 parser = new VCardParser_V21();
        VNodeBuilder builder = new VNodeBuilder();
        InputStream is = getContext().getResources().openRawResource(R.raw.v21_backslash);
        assertEquals(true, parser.parse(is,"ISO-8859-1", builder));
        is.close();
        assertEquals(1, builder.vNodeList.size());
        PropertyNodesVerifier verifier = new PropertyNodesVerifier()
            .addPropertyNode("VERSION", "2.1", null, null, null, null, null)
            .addPropertyNode("N", ";A;B\\;C\\;;D;:E;\\\\;",
                    Arrays.asList("", "A;B\\", "C\\;", "D", ":E", "\\\\", ""),
                    null, null, null, null)
            .addPropertyNode("FN", "A;B\\C\\;D:E\\\\", null, null, null, null, null);
        verifier.verify(builder.vNodeList.get(0));
    }

    /**
     * Tests ContactStruct correctly ignores redundant fields in "N" property values and
     * inserts name related data.
     */
    public void testV21BackslashCase() throws IOException, VCardException {
        ContactStructVerifier verifier = new ContactStructVerifier(
                R.raw.v21_backslash, VCardConfig.VCARD_TYPE_V21_GENERIC);
        ContentValues contentValues =
            verifier.createExpected(StructuredName.CONTENT_ITEM_TYPE);
        // FAMILY_NAME is empty and removed in this test...
        contentValues.put(StructuredName.GIVEN_NAME, "A;B\\");
        contentValues.put(StructuredName.MIDDLE_NAME, "C\\;");
        contentValues.put(StructuredName.PREFIX, "D");
        contentValues.put(StructuredName.SUFFIX, ":E");
        contentValues.put(StructuredName.DISPLAY_NAME, "A;B\\C\\;D:E\\\\");
        verifier.verify();
    }

    public void testOrgBeforTitle() throws IOException, VCardException {
        ContactStructVerifier verifier = new ContactStructVerifier(
                R.raw.v21_org_before_title, VCardConfig.VCARD_TYPE_V21_GENERIC);
        ContentValues contentValues =
            verifier.createExpected(StructuredName.CONTENT_ITEM_TYPE);
        contentValues.put(StructuredName.DISPLAY_NAME, "Normal Guy");

        contentValues = verifier.createExpected(Organization.CONTENT_ITEM_TYPE);
        contentValues.put(Organization.COMPANY, "Company");
        contentValues.put(Organization.DEPARTMENT, "Organization Devision Room Sheet No.");
        contentValues.put(Organization.TITLE, "Excellent Janitor");
        contentValues.put(Organization.TYPE, Organization.TYPE_WORK);
        verifier.verify();
    }

    public void testTitleBeforOrg() throws IOException, VCardException {
        ContactStructVerifier verifier = new ContactStructVerifier(
                R.raw.v21_title_before_org, VCardConfig.VCARD_TYPE_V21_GENERIC);
        ContentValues contentValues =
            verifier.createExpected(StructuredName.CONTENT_ITEM_TYPE);
        contentValues.put(StructuredName.DISPLAY_NAME, "Nice Guy");

        contentValues = verifier.createExpected(Organization.CONTENT_ITEM_TYPE);
        contentValues.put(Organization.COMPANY, "Marverous");
        contentValues.put(Organization.DEPARTMENT, "Perfect Great Good Bad Poor");
        contentValues.put(Organization.TITLE, "Cool Title");
        contentValues.put(Organization.TYPE, Organization.TYPE_WORK);
        verifier.verify();
    }

    /**
     * Verifies that vCard importer correctly interpret "PREF" attribute to IS_PRIMARY.
     * The data contain three cases: one "PREF", no "PREF" and multiple "PREF", in each type.
     */
    public void testV21PrefToIsPrimary() throws IOException, VCardException {
        ContactStructVerifier verifier = new ContactStructVerifier(
                R.raw.v21_pref_handling, VCardConfig.VCARD_TYPE_V21_GENERIC);
        ContentValues contentValues =
            verifier.createExpected(StructuredName.CONTENT_ITEM_TYPE);
        contentValues.put(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE);
        contentValues.put(StructuredName.DISPLAY_NAME, "Smith");

        contentValues = verifier.createExpected(Phone.CONTENT_ITEM_TYPE);
        contentValues.put(Phone.NUMBER, "1");
        contentValues.put(Phone.TYPE, Phone.TYPE_HOME);

        contentValues = verifier.createExpected(Phone.CONTENT_ITEM_TYPE);
        contentValues.put(Phone.NUMBER, "2");
        contentValues.put(Phone.TYPE, Phone.TYPE_WORK);
        contentValues.put(Phone.IS_PRIMARY, 1);

        contentValues = verifier.createExpected(Phone.CONTENT_ITEM_TYPE);
        contentValues.put(Phone.NUMBER, "3");
        contentValues.put(Phone.TYPE, Phone.TYPE_ISDN);

        contentValues = verifier.createExpected(Email.CONTENT_ITEM_TYPE);
        contentValues.put(Email.DATA, "test@example.com");
        contentValues.put(Email.TYPE, Email.TYPE_HOME);
        contentValues.put(Email.IS_PRIMARY, 1);

        contentValues = verifier.createExpected(Email.CONTENT_ITEM_TYPE);
        contentValues.put(Email.DATA, "test2@examination.com");
        contentValues.put(Email.TYPE, Email.TYPE_MOBILE);
        contentValues.put(Email.IS_PRIMARY, 1);

        contentValues = verifier.createExpected(Organization.CONTENT_ITEM_TYPE);
        contentValues.put(Organization.COMPANY, "Company");
        contentValues.put(Organization.TITLE, "Engineer");
        contentValues.put(Organization.TYPE, Organization.TYPE_WORK);

        contentValues = verifier.createExpected(Organization.CONTENT_ITEM_TYPE);
        contentValues.put(Organization.COMPANY, "Mystery");
        contentValues.put(Organization.TITLE, "Blogger");
        contentValues.put(Organization.TYPE, Organization.TYPE_WORK);

        contentValues = verifier.createExpected(Organization.CONTENT_ITEM_TYPE);
        contentValues.put(Organization.COMPANY, "Poetry");
        contentValues.put(Organization.TITLE, "Poet");
        contentValues.put(Organization.TYPE, Organization.TYPE_WORK);
        verifier.verify();
    }

    /**
     * Tests all the properties in a complicated vCard are correctly parsed by the VCardParser.
     */
    public void testV21ComplicatedCase_Parsing() throws IOException, VCardException {
        VCardParser_V21 parser = new VCardParser_V21();
        VNodeBuilder builder = new VNodeBuilder();
        InputStream is = getContext().getResources().openRawResource(R.raw.v21_complicated);
        assertEquals(true, parser.parse(is,"ISO-8859-1", builder));
        is.close();
        assertEquals(1, builder.vNodeList.size());
        ContentValues contentValuesForQP = new ContentValues();
        contentValuesForQP.put("ENCODING", "QUOTED-PRINTABLE");
        ContentValues contentValuesForPhoto = new ContentValues();
        contentValuesForPhoto.put("ENCODING", "BASE64");
        PropertyNodesVerifier verifier = new PropertyNodesVerifier()
            .addPropertyNode("VERSION", "2.1", null, null, null, null, null)
            .addPropertyNode("N", "Gump;Forrest;Hoge;Pos;Tao",
                    Arrays.asList("Gump", "Forrest", "Hoge", "Pos", "Tao"),
                    null, null, null, null)
            .addPropertyNode("FN", "Joe Due", null, null, null, null, null)
            .addPropertyNode("ORG", "Gump Shrimp Co.;Sales Dept.;Manager;Fish keeper",
                    Arrays.asList("Gump Shrimp Co.", "Sales Dept.;Manager", "Fish keeper"),
                    null, null, null, null)
            .addPropertyNode("ROLE", "Fish Cake Keeper!", null, null, null, null, null)
            .addPropertyNode("TITLE", "Shrimp Man", null, null, null, null, null)
            .addPropertyNode("X-CLASS", "PUBLIC", null, null, null, null, null)
            .addPropertyNode("TEL", "(111) 555-1212", null, null, null,
                    new HashSet<String>(Arrays.asList("WORK", "VOICE")), null)
            .addPropertyNode("TEL", "(404) 555-1212", null, null, null,
                    new HashSet<String>(Arrays.asList("HOME", "VOICE")), null)
            .addPropertyNode("TEL", "0311111111", null, null, null,
                    new HashSet<String>(Arrays.asList("CELL")), null)
            .addPropertyNode("TEL", "0322222222", null, null, null,
                    new HashSet<String>(Arrays.asList("VIDEO")), null)
            .addPropertyNode("TEL", "0333333333", null, null, null,
                    new HashSet<String>(Arrays.asList("VOICE")), null)     
            .addPropertyNode("ADR", ";;100 Waters Edge;Baytown;LA;30314;United States of America",
                    Arrays.asList("", "", "100 Waters Edge", "Baytown",
                            "LA", "30314", "United States of America"),
                            null, null, new HashSet<String>(Arrays.asList("WORK")), null)
            .addPropertyNode("LABEL",
                    "100 Waters Edge\r\nBaytown, LA 30314\r\nUnited  States of America",
                    null, null, contentValuesForQP,
                    new HashSet<String>(Arrays.asList("WORK")), null)
            .addPropertyNode("ADR",
                    ";;42 Plantation St.;Baytown;LA;30314;United States of America",
                    Arrays.asList("", "", "42 Plantation St.", "Baytown",
                            "LA", "30314", "United States of America"), null, null,
                    new HashSet<String>(Arrays.asList("HOME")), null)
            .addPropertyNode("LABEL",
                    "42 Plantation St.\r\nBaytown, LA 30314\r\nUnited  States of America",
                    null, null, contentValuesForQP,
                    new HashSet<String>(Arrays.asList("HOME")), null)
            .addPropertyNode("EMAIL", "forrestgump@walladalla.com",
                    null, null, null,
                    new HashSet<String>(Arrays.asList("PREF", "INTERNET")), null)
            .addPropertyNode("EMAIL", "cell@example.com", null, null, null,
                    new HashSet<String>(Arrays.asList("CELL")), null)
            .addPropertyNode("NOTE", "The following note is the example from RFC 2045.",
                    null, null, null, null, null)
            .addPropertyNode("NOTE",
                    "Now's the time for all folk to come to the aid of their country.",
                    null, null, contentValuesForQP, null, null)
            .addPropertyNode("PHOTO", null,
                    null, sPhotoByteArrayForComplicatedCase, contentValuesForPhoto,
                    new HashSet<String>(Arrays.asList("JPEG")), null)
            .addPropertyNode("X-ATTRIBUTE", "Some String", null, null, null, null, null)
            .addPropertyNode("BDAY", "19800101", null, null, null, null, null)
            .addPropertyNode("GEO", "35.6563854,139.6994233", null, null, null, null, null)
            .addPropertyNode("URL", "http://www.example.com/", null, null, null, null, null)
            .addPropertyNode("REV", "20080424T195243Z", null, null, null, null, null);
        verifier.verify(builder.vNodeList.get(0));
    }

    /**
     * Checks ContactStruct correctly inserts values in a complicated vCard
     * into ContentResolver.
     */
    public void testV21ComplicatedCase() throws IOException, VCardException {
        ContactStructVerifier verifier = new ContactStructVerifier(
                R.raw.v21_complicated, VCardConfig.VCARD_TYPE_V21_GENERIC);
        ContentValues contentValues =
            verifier.createExpected(StructuredName.CONTENT_ITEM_TYPE);
        contentValues.put(StructuredName.FAMILY_NAME, "Gump");
        contentValues.put(StructuredName.GIVEN_NAME, "Forrest");
        contentValues.put(StructuredName.MIDDLE_NAME, "Hoge");
        contentValues.put(StructuredName.PREFIX, "Pos");
        contentValues.put(StructuredName.SUFFIX, "Tao");
        contentValues.put(StructuredName.DISPLAY_NAME, "Joe Due");
        
        contentValues = verifier.createExpected(Organization.CONTENT_ITEM_TYPE);
        contentValues.put(Organization.TYPE, Organization.TYPE_WORK);
        contentValues.put(Organization.COMPANY, "Gump Shrimp Co.");
        contentValues.put(Organization.DEPARTMENT, "Sales Dept.;Manager Fish keeper");
        contentValues.put(Organization.TITLE, "Shrimp Man");

        contentValues = verifier.createExpected(Phone.CONTENT_ITEM_TYPE);
        contentValues.put(Phone.TYPE, Phone.TYPE_WORK);
        // Phone number is expected to be formated with NAMP format in default.
        contentValues.put(Phone.NUMBER, "111-555-1212");

        contentValues = verifier.createExpected(Phone.CONTENT_ITEM_TYPE);
        contentValues.put(Phone.TYPE, Phone.TYPE_HOME);
        contentValues.put(Phone.NUMBER, "404-555-1212");

        contentValues = verifier.createExpected(Phone.CONTENT_ITEM_TYPE);
        contentValues.put(Phone.TYPE, Phone.TYPE_MOBILE);
        contentValues.put(Phone.NUMBER, "031-111-1111");

        contentValues = verifier.createExpected(Phone.CONTENT_ITEM_TYPE);
        contentValues.put(Phone.TYPE, Phone.TYPE_CUSTOM);
        contentValues.put(Phone.LABEL, "VIDEO");
        contentValues.put(Phone.NUMBER, "032-222-2222");

        contentValues = verifier.createExpected(Phone.CONTENT_ITEM_TYPE);
        contentValues.put(Phone.TYPE, Phone.TYPE_CUSTOM);
        contentValues.put(Phone.LABEL, "VOICE");
        contentValues.put(Phone.NUMBER, "033-333-3333");

        contentValues = verifier.createExpected(StructuredPostal.CONTENT_ITEM_TYPE);
        contentValues.put(StructuredPostal.TYPE, StructuredPostal.TYPE_WORK);
        contentValues.put(StructuredPostal.COUNTRY, "United States of America");
        contentValues.put(StructuredPostal.POSTCODE, "30314");
        contentValues.put(StructuredPostal.REGION, "LA");
        contentValues.put(StructuredPostal.CITY, "Baytown");
        contentValues.put(StructuredPostal.STREET, "100 Waters Edge");
        contentValues.put(StructuredPostal.FORMATTED_ADDRESS,
                "100 Waters Edge Baytown LA 30314 United States of America");

        contentValues = verifier.createExpected(StructuredPostal.CONTENT_ITEM_TYPE);
        contentValues.put(StructuredPostal.TYPE, StructuredPostal.TYPE_HOME);
        contentValues.put(StructuredPostal.COUNTRY, "United States of America");
        contentValues.put(StructuredPostal.POSTCODE, "30314");
        contentValues.put(StructuredPostal.REGION, "LA");
        contentValues.put(StructuredPostal.CITY, "Baytown");
        contentValues.put(StructuredPostal.STREET, "42 Plantation St.");
        contentValues.put(StructuredPostal.FORMATTED_ADDRESS,
                "42 Plantation St. Baytown LA 30314 United States of America");

        contentValues = verifier.createExpected(Email.CONTENT_ITEM_TYPE);
        // "TYPE=INTERNET" -> TYPE_CUSTOM + the label "INTERNET"
        contentValues.put(Email.TYPE, Email.TYPE_CUSTOM);
        contentValues.put(Email.LABEL, "INTERNET");
        contentValues.put(Email.DATA, "forrestgump@walladalla.com");
        contentValues.put(Email.IS_PRIMARY, 1);

        contentValues = verifier.createExpected(Email.CONTENT_ITEM_TYPE);
        contentValues.put(Email.TYPE, Email.TYPE_MOBILE);
        contentValues.put(Email.DATA, "cell@example.com");

        contentValues = verifier.createExpected(Note.CONTENT_ITEM_TYPE);
        contentValues.put(Note.NOTE, "The following note is the example from RFC 2045.");

        contentValues = verifier.createExpected(Note.CONTENT_ITEM_TYPE);
        contentValues.put(Note.NOTE,
                "Now's the time for all folk to come to the aid of their country.");

        contentValues = verifier.createExpected(Photo.CONTENT_ITEM_TYPE);
        // No information about its image format can be inserted.
        contentValues.put(Photo.PHOTO, sPhotoByteArrayForComplicatedCase);

        contentValues = verifier.createExpected(Event.CONTENT_ITEM_TYPE);
        contentValues.put(Event.START_DATE, "19800101");
        contentValues.put(Event.TYPE, Event.TYPE_BIRTHDAY);

        contentValues = verifier.createExpected(Website.CONTENT_ITEM_TYPE);
        contentValues.put(Website.URL, "http://www.example.com/");
        contentValues.put(Website.TYPE, Website.TYPE_HOMEPAGE);
        verifier.verify();
    }

    public void testV30Simple_Parsing() throws IOException, VCardException {
        VCardParser_V21 parser = new VCardParser_V30();
        VNodeBuilder builder = new VNodeBuilder();
        InputStream is = getContext().getResources().openRawResource(R.raw.v30_simple);
        assertEquals(true, parser.parse(is,"ISO-8859-1", builder));
        is.close();
        assertEquals(1, builder.vNodeList.size());
        PropertyNodesVerifier verifier = new PropertyNodesVerifier()
            .addPropertyNode("VERSION", "3.0", null, null, null, null, null)
            .addPropertyNode("FN", "And Roid", null, null, null, null, null)
            .addPropertyNode("N", "And;Roid;;;", Arrays.asList("And", "Roid", "", "", ""),
                    null, null, null, null)
            .addPropertyNode("ORG", "Open;Handset; Alliance",
                    Arrays.asList("Open", "Handset", " Alliance"),
                    null, null, null, null)
            .addPropertyNode("SORT-STRING", "android", null, null, null, null, null)
            .addPropertyNode("TEL", "0300000000", null, null, null,
                        new HashSet<String>(Arrays.asList("PREF", "VOICE")), null)
            .addPropertyNode("CLASS", "PUBLIC", null, null, null, null, null)
            .addPropertyNode("X-GNO", "0", null, null, null, null, null)
            .addPropertyNode("X-GN", "group0", null, null, null, null, null)
            .addPropertyNode("X-REDUCTION", "0", null, null, null, null, null)
            .addPropertyNode("REV", "20081031T065854Z", null, null, null, null, null);
        verifier.verify(builder.vNodeList.get(0));
    }

    public void testV30Simple() throws IOException, VCardException {
        ContactStructVerifier verifier = new ContactStructVerifier(
                R.raw.v30_simple, VCardConfig.VCARD_TYPE_V30_GENERIC);
        ContentValues contentValues =
            verifier.createExpected(StructuredName.CONTENT_ITEM_TYPE);
        contentValues.put(StructuredName.FAMILY_NAME, "And");
        contentValues.put(StructuredName.GIVEN_NAME, "Roid");
        contentValues.put(StructuredName.DISPLAY_NAME, "And Roid");

        contentValues = verifier.createExpected(Organization.CONTENT_ITEM_TYPE);
        contentValues.put(Organization.COMPANY, "Open");
        contentValues.put(Organization.DEPARTMENT, "Handset  Alliance");
        contentValues.put(Organization.TYPE, Organization.TYPE_WORK);

        contentValues = verifier.createExpected(Phone.CONTENT_ITEM_TYPE);
        contentValues.put(Phone.TYPE, Phone.TYPE_CUSTOM);
        contentValues.put(Phone.LABEL, "VOICE");
        contentValues.put(Phone.NUMBER, "030-000-0000");
        contentValues.put(Phone.IS_PRIMARY, 1);
        verifier.verify();
    }

    public void testV21Japanese1_Parsing() throws IOException, VCardException {
        VCardParser_V21 parser = new VCardParser_V21();
        VNodeBuilder builder = new VNodeBuilder();
        InputStream is = getContext().getResources().openRawResource(R.raw.v21_japanese_1);
        assertEquals(true, parser.parse(is,"ISO-8859-1", builder));
        is.close();
        assertEquals(1, builder.vNodeList.size());
        ContentValues contentValuesForShiftJis = new ContentValues();
        contentValuesForShiftJis.put("CHARSET", "SHIFT_JIS");
        ContentValues contentValuesForQP = new ContentValues();
        contentValuesForQP.put("ENCODING", "QUOTED-PRINTABLE");
        contentValuesForQP.put("CHARSET", "SHIFT_JIS");
        // Though Japanese careers append ";;;;" at the end of the value of "SOUND",
        // vCard 2.1/3.0 specification does not allow multiple values.
        // Do not need to handle it as multiple values.
        PropertyNodesVerifier verifier = new PropertyNodesVerifier()
            .addPropertyNode("VERSION", "2.1", null, null, null, null, null)
            .addPropertyNode("N", "\u5B89\u85E4\u30ED\u30A4\u30C9;;;;",
                    Arrays.asList("\u5B89\u85E4\u30ED\u30A4\u30C9", "", "", "", ""),
                    null, contentValuesForShiftJis, null, null)
            .addPropertyNode("SOUND", "\uFF71\uFF9D\uFF84\uFF9E\uFF73\uFF9B\uFF72\uFF84\uFF9E;;;;",
                    null, null, contentValuesForShiftJis,
                    new HashSet<String>(Arrays.asList("X-IRMC-N")), null)
            .addPropertyNode("TEL", "0300000000", null, null, null,
                    new HashSet<String>(Arrays.asList("VOICE", "PREF")), null);
        verifier.verify(builder.vNodeList.get(0));
    }

    private void testV21Japanese1Common(ContactStructVerifier verifier, boolean japanese)
            throws IOException, VCardException {
        ContentValues contentValues =
            verifier.createExpected(StructuredName.CONTENT_ITEM_TYPE);
        contentValues.put(StructuredName.FAMILY_NAME, "\u5B89\u85E4\u30ED\u30A4\u30C9");
        contentValues.put(StructuredName.DISPLAY_NAME, "\u5B89\u85E4\u30ED\u30A4\u30C9");
        // While vCard parser does not split "SOUND" property values, ContactStruct care it.
        contentValues.put(StructuredName.PHONETIC_FAMILY_NAME,
                "\uFF71\uFF9D\uFF84\uFF9E\uFF73\uFF9B\uFF72\uFF84\uFF9E");

        contentValues = verifier.createExpected(Phone.CONTENT_ITEM_TYPE);
        // Phone number formatting is different.
        if (japanese) {
            contentValues.put(Phone.NUMBER, "03-0000-0000");
        } else {
            contentValues.put(Phone.NUMBER, "030-000-0000");
        }
        contentValues.put(Phone.TYPE, Phone.TYPE_CUSTOM);
        contentValues.put(Phone.LABEL, "VOICE");
        contentValues.put(Phone.IS_PRIMARY, 1);
        verifier.verify();
    }
    /**
     * Verifies vCard with Japanese can be parsed correctly with VCARD_TYPE_V21_GENERIC. 
     */
    public void testV21Japanese1_Type_Generic() throws IOException, VCardException {
        ContactStructVerifier verifier = new ContactStructVerifier(
                R.raw.v21_japanese_1, VCardConfig.VCARD_TYPE_V21_GENERIC);
        testV21Japanese1Common(verifier, false);
    }

    /**
     * Verifies vCard with Japanese can be parsed correctly with VCARD_TYPE_V21_JAPANESE.
     */
    public void testV21Japanese1_Type_Japanese() throws IOException, VCardException {
        ContactStructVerifier verifier = new ContactStructVerifier(
                R.raw.v21_japanese_1, VCardConfig.VCARD_TYPE_V21_JAPANESE);
        testV21Japanese1Common(verifier, true);
    }

    /**
     * Verifies vCard with Japanese can be parsed correctly with VCARD_TYPE_V21_JAPANESE_UTF8,
     * since vCard 2.1 specifies the charset of each line if it contains non-Ascii.
     */
    public void testV21Japanese1_Type_Japanese_Utf8() throws IOException, VCardException {
        ContactStructVerifier verifier = new ContactStructVerifier(
                R.raw.v21_japanese_1, VCardConfig.VCARD_TYPE_V21_JAPANESE_UTF8);
        testV21Japanese1Common(verifier, true);
    }

    public void testV21Japanese2_Parsing() throws IOException, VCardException {
        VCardParser_V21 parser = new VCardParser_V21();
        VNodeBuilder builder = new VNodeBuilder();
        InputStream is = getContext().getResources().openRawResource(R.raw.v21_japanese_2);
        assertEquals(true, parser.parse(is,"ISO-8859-1", builder));
        is.close();
        assertEquals(1, builder.vNodeList.size());
        ContentValues contentValuesForShiftJis = new ContentValues();
        contentValuesForShiftJis.put("CHARSET", "SHIFT_JIS");
        ContentValues contentValuesForQP = new ContentValues();
        contentValuesForQP.put("ENCODING", "QUOTED-PRINTABLE");
        contentValuesForQP.put("CHARSET", "SHIFT_JIS");
        PropertyNodesVerifier verifier = new PropertyNodesVerifier()
            .addPropertyNode("VERSION", "2.1", null, null, null, null, null)
            .addPropertyNode("N", "\u5B89\u85E4;\u30ED\u30A4\u30C9\u0031;;;",
                    Arrays.asList("\u5B89\u85E4", "\u30ED\u30A4\u30C9\u0031",
                            "", "", ""),
                    null, contentValuesForShiftJis, null, null)
            .addPropertyNode("FN", "\u5B89\u85E4\u0020\u30ED\u30A4\u30C9\u0020\u0031",
                    null, null, contentValuesForShiftJis, null, null)
            .addPropertyNode("SOUND",
                    "\uFF71\uFF9D\uFF84\uFF9E\uFF73;\uFF9B\uFF72\uFF84\uFF9E\u0031;;;",
                    null, null, contentValuesForShiftJis,
                    new HashSet<String>(Arrays.asList("X-IRMC-N")), null)
            .addPropertyNode("ADR",
                    ";\u6771\u4EAC\u90FD\u6E0B\u8C37\u533A\u685C" +
                    "\u4E18\u753A\u0032\u0036\u002D\u0031\u30BB" +
                    "\u30EB\u30EA\u30A2\u30F3\u30BF\u30EF\u30FC\u0036" +
                    "\u968E;;;;150-8512;",
                    Arrays.asList("",
                            "\u6771\u4EAC\u90FD\u6E0B\u8C37\u533A\u685C" +
                            "\u4E18\u753A\u0032\u0036\u002D\u0031\u30BB" +
                            "\u30EB\u30EA\u30A2\u30F3\u30BF\u30EF\u30FC" +
                            "\u0036\u968E", "", "", "", "150-8512", ""),
                    null, contentValuesForQP, new HashSet<String>(Arrays.asList("HOME")), null)
            .addPropertyNode("NOTE", "\u30E1\u30E2", null, null, contentValuesForQP, null, null);
        verifier.verify(builder.vNodeList.get(0));
    }

    public void testV21Japanese2_Type_Generic() throws IOException, VCardException {
        ContactStructVerifier verifier = new ContactStructVerifier(
                R.raw.v21_japanese_2, VCardConfig.VCARD_TYPE_V21_GENERIC);
        ContentValues contentValues =
            verifier.createExpected(StructuredName.CONTENT_ITEM_TYPE);
        contentValues.put(StructuredName.FAMILY_NAME, "\u5B89\u85E4");
        contentValues.put(StructuredName.GIVEN_NAME, "\u30ED\u30A4\u30C9\u0031");
        contentValues.put(StructuredName.DISPLAY_NAME,
                "\u5B89\u85E4\u0020\u30ED\u30A4\u30C9\u0020\u0031");
        // ContactStruct should correctly split "SOUND" property into several elements,
        // even though VCardParser side does not care it. 
        contentValues.put(StructuredName.PHONETIC_FAMILY_NAME,
                "\uFF71\uFF9D\uFF84\uFF9E\uFF73");
        contentValues.put(StructuredName.PHONETIC_GIVEN_NAME,
                "\uFF9B\uFF72\uFF84\uFF9E\u0031");

        contentValues = verifier.createExpected(StructuredPostal.CONTENT_ITEM_TYPE);
        contentValues.put(StructuredPostal.POSTCODE, "150-8512");
        contentValues.put(StructuredPostal.NEIGHBORHOOD,
                "\u6771\u4EAC\u90FD\u6E0B\u8C37\u533A\u685C" +
                "\u4E18\u753A\u0032\u0036\u002D\u0031\u30BB" +
                "\u30EB\u30EA\u30A2\u30F3\u30BF\u30EF\u30FC" +
                "\u0036\u968E");
        contentValues.put(StructuredPostal.FORMATTED_ADDRESS,
                "\u6771\u4EAC\u90FD\u6E0B\u8C37\u533A\u685C" +
                "\u4E18\u753A\u0032\u0036\u002D\u0031\u30BB" +
                "\u30EB\u30EA\u30A2\u30F3\u30BF\u30EF\u30FC" +
                "\u0036\u968E 150-8512");
        contentValues.put(StructuredPostal.TYPE, StructuredPostal.TYPE_HOME);
        contentValues = verifier.createExpected(Note.CONTENT_ITEM_TYPE);
        contentValues.put(Note.NOTE, "\u30E1\u30E2");
        verifier.verify();
    }

    // Following tests are old ones, though they still work fine.
    
    public void testV21MultipleEntryCase() throws IOException, VCardException {
        VCardParser_V21 parser = new VCardParser_V21();
        VNodeBuilder builder = new VNodeBuilder();
        InputStream is = getContext().getResources().openRawResource(R.raw.v21_multiple_entry);
        assertEquals(true, parser.parse(is,"ISO-8859-1", builder));
        is.close();
        assertEquals(3, builder.vNodeList.size());
        ContentValues contentValuesForShiftJis = new ContentValues();
        contentValuesForShiftJis.put("CHARSET", "SHIFT_JIS");
        PropertyNodesVerifier verifier = new PropertyNodesVerifier()
            .addPropertyNode("VERSION", "2.1", null, null, null, null, null)
            .addPropertyNode("N", "\u5B89\u85E4\u30ED\u30A4\u30C9\u0033;;;;",
                    Arrays.asList("\u5B89\u85E4\u30ED\u30A4\u30C9\u0033", "", "", "", ""),
                    null, contentValuesForShiftJis, null, null)
            .addPropertyNode("SOUND",
                    "\uFF71\uFF9D\uFF84\uFF9E\uFF73\uFF9B\uFF72\uFF84\uFF9E\u0033;;;;",
                    null, null, contentValuesForShiftJis,
                    new HashSet<String>(Arrays.asList("X-IRMC-N")), null)
            .addPropertyNode("TEL", "9", null, null, null,
                    new HashSet<String>(Arrays.asList("X-NEC-SECRET")), null)
            .addPropertyNode("TEL", "10", null, null, null,
                    new HashSet<String>(Arrays.asList("X-NEC-HOTEL")), null)
            .addPropertyNode("TEL", "11", null, null, null,
                    new HashSet<String>(Arrays.asList("X-NEC-SCHOOL")), null)
            .addPropertyNode("TEL", "12", null, null, null,
                    new HashSet<String>(Arrays.asList("FAX", "HOME")), null);
        verifier.verify(builder.vNodeList.get(0));
        
        verifier = new PropertyNodesVerifier()
            .addPropertyNode("VERSION", "2.1", null, null, null, null, null)
            .addPropertyNode("N", "\u5B89\u85E4\u30ED\u30A4\u30C9\u0034;;;;",
                    Arrays.asList("\u5B89\u85E4\u30ED\u30A4\u30C9\u0034", "", "", "", ""),
                    null, contentValuesForShiftJis, null, null)
            .addPropertyNode("SOUND",
                    "\uFF71\uFF9D\uFF84\uFF9E\uFF73\uFF9B\uFF72\uFF84\uFF9E\u0034;;;;",
                    null, null, contentValuesForShiftJis,
                    new HashSet<String>(Arrays.asList("X-IRMC-N")), null)
            .addPropertyNode("TEL", "13", null, null, null,
                    new HashSet<String>(Arrays.asList("MODEM")), null)
            .addPropertyNode("TEL", "14", null, null, null,
                    new HashSet<String>(Arrays.asList("PAGER")), null)
            .addPropertyNode("TEL", "15", null, null, null,
                    new HashSet<String>(Arrays.asList("X-NEC-FAMILY")), null)
            .addPropertyNode("TEL", "16", null, null, null,
                    new HashSet<String>(Arrays.asList("X-NEC-GIRL")), null);
        verifier.verify(builder.vNodeList.get(1));
        verifier = new PropertyNodesVerifier()
            .addPropertyNode("VERSION", "2.1", null, null, null, null, null)
            .addPropertyNode("N", "\u5B89\u85E4\u30ED\u30A4\u30C9\u0035;;;;",
                    Arrays.asList("\u5B89\u85E4\u30ED\u30A4\u30C9\u0035", "", "", "", ""),
                    null, contentValuesForShiftJis, null, null)
            .addPropertyNode("SOUND",
                    "\uFF71\uFF9D\uFF84\uFF9E\uFF73\uFF9B\uFF72\uFF84\uFF9E\u0035;;;;",
                    null, null, contentValuesForShiftJis,
                    new HashSet<String>(Arrays.asList("X-IRMC-N")), null)
            .addPropertyNode("TEL", "17", null, null, null,
                    new HashSet<String>(Arrays.asList("X-NEC-BOY")), null)
            .addPropertyNode("TEL", "18", null, null, null,
                    new HashSet<String>(Arrays.asList("X-NEC-FRIEND")), null)
            .addPropertyNode("TEL", "19", null, null, null,
                    new HashSet<String>(Arrays.asList("X-NEC-PHS")), null)
            .addPropertyNode("TEL", "20", null, null, null,
                    new HashSet<String>(Arrays.asList("X-NEC-RESTAURANT")), null);
        verifier.verify(builder.vNodeList.get(2));
    }
}
