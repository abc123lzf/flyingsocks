/*
 * Copyright (c) 2019 abc123lzf <abc123lzf@126.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.lzf.flyingsocks.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * @author lzf abc123lzf@126.com
 * @since 2021/3/4 20:04
 */
public class DnsResponseMessage extends DnsMessage {

    protected final List<Record> answers = new ArrayList<>();

    protected final List<Record> authority = new ArrayList<>();

    protected final List<Record> additionalInfomation = new ArrayList<>();


    public DnsResponseMessage(short transactionId) {
        super(transactionId);
    }


    public DnsResponseMessage(ByteBuf buf) throws SerializationException {
        super(buf);
    }


    public void setRCODE(byte code) {
        super.setRCODE(code);
    }

    @Override
    public void setOpcode(byte code) {
        super.setOpcode(code);
    }

    @Override
    public void setAA(boolean open) {
        super.setAA(open);
    }

    @Override
    public void setRA(boolean open) {
        super.setRA(open);
    }

    @Override
    public void setRD(boolean open) {
        super.setRD(open);
    }

    @Override
    public void setTC(boolean open) {
        super.setTC(open);
    }

    @Override
    public void setQR(boolean open) {
        super.setQR(open);
    }

    @Override
    protected ByteBuf serializeBody(ByteBufAllocator allocator) throws SerializationException {
        int size = 0;
        for (Record r : answers) {
            size += r.getDomain().length() + 1;
            size += 10;
            size += r.getDataLength();
        }

        for (Record r : authority) {
            size += r.getDomain().length() + 1;
            size += 10;
            size += r.getDataLength();
        }

        for (Record r : additionalInfomation) {
            size += r.getDomain().length() + 1;
            size += 10;
            size += r.getDataLength();
        }

        ByteBuf body = allocator.directBuffer(size);

        final Consumer<Record> consumer = r -> {
            body.writeCharSequence(r.getDomain(), StandardCharsets.US_ASCII);
            body.writeByte(0);
            body.writeShort(r.getType());
            body.writeShort(r.getKlass());
            body.writeInt(r.getTTL());
            body.writeShort(r.getDataLength());
            if (r.getDataLength() > 0) {
                body.writeBytes(r.getData());
            }
        };
        answers.forEach(consumer);
        authority.forEach(consumer);
        additionalInfomation.forEach(consumer);
        return body;
    }

    @Override
    protected void deserializeBody(ByteBuf buf) throws SerializationException {
        try {
            int cnt = Short.toUnsignedInt(super.answerCount);
            for (int i = 0; i < cnt; i++) {
                answers.add(readResource(buf));
            }

            cnt = Short.toUnsignedInt(super.authorityCount);
            for (int i = 0; i < cnt; i++) {
                authority.add(readResource(buf));
            }

            cnt = Short.toUnsignedInt(super.additionalInfomationCount);
            for (int i = 0; i < cnt; i++) {
                additionalInfomation.add(readResource(buf));
            }
        } catch (IndexOutOfBoundsException e) {
            throw new SerializationException(getClass(), e);
        }
    }


    public void addAnswer(Record record) {
        answers.add(Objects.requireNonNull(record));
        super.answerCount++;
    }


    public void addAuthority(Record record) {
        authority.add(Objects.requireNonNull(record));
        super.authorityCount++;
    }


    public void addAdditionalInfomation(Record record) {
        additionalInfomation.add(Objects.requireNonNull(record));
        super.additionalInfomationCount++;
    }


    public List<Record> getAnswers() {
        return Collections.unmodifiableList(answers);
    }

    public List<Record> getAuthority() {
        return Collections.unmodifiableList(authority);
    }

    public List<Record> getAdditionalInfomation() {
        return Collections.unmodifiableList(additionalInfomation);
    }
}
