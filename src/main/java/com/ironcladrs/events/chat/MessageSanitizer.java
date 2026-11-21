/*
BSD 2-Clause License

Copyright (c) 2023, Joshua Blackman

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this
   list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
   this list of conditions and the following disclaimer in the documentation
   and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package com.ironcladrs.events.chat;

import java.util.regex.*;

/** Adapted from Clan Chat Webhooks; its BSD-2 notice is reproduced in NOTICE. */
public final class MessageSanitizer
{
    private static final Pattern TEMPLATE = Pattern.compile("<(?<name>str_\\w+)=(?<value>[^>]*)>");
    private MessageSanitizer() {}
    public static String sanitize(String input)
    {
        String text = input == null ? "" : input.replace(' ', ' ');
        Matcher matcher = TEMPLATE.matcher(text);
        while (matcher.find())
        {
            String tag = matcher.group("name"), value = matcher.group("value");
            text = matcher.replaceFirst("").replace("<" + tag + ">", value); matcher = TEMPLATE.matcher(text);
        }
        text = text.replace("<lt>", "\u0001").replace("<gt>", "\u0002").replaceAll("<[^>]*>", "").replace("\u0001", "<").replace("\u0002", ">");
        text = text.replaceAll("[\\p{Cntrl}&&[^\\t]]", " ");
        return text.substring(0, Math.min(500, text.length()));
    }
}
