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

/**
 * DNS查询请求
 *
 * @author lzf abc123lzf@126.com
 * @since 2021/3/4 19:58
 */
public class DnsQueryMessage extends DnsMessage {

    /**
     * 问题列表，一般只有一个
     */
    protected List<Question> questions;


    public DnsQueryMessage(short transactionId) {
        super(transactionId);
        setQR(false);
    }

    public DnsQueryMessage(ByteBuf buf) throws SerializationException {
        super(buf);
    }

    @Override
    protected ByteBuf serializeBody(ByteBufAllocator allocator) throws SerializationException {
        if (questions.isEmpty()) {
            throw new SerializationException(getClass(), "No question");
        }

        int size = 0;
        for (Question q : questions) {
            size += q.getName().length() + 1;
            size += 4;
        }

        ByteBuf body = allocator.directBuffer(size);
        for (Question q : questions) {
            body.writeCharSequence(q.getName(), StandardCharsets.US_ASCII);
            body.writeByte(0);
            body.writeShort(q.getType());
            body.writeShort(q.getKlass());
        }

        return body;
    }

    @Override
    protected void deserializeBody(ByteBuf buf) throws SerializationException {
        int size = Short.toUnsignedInt(super.questionCount);
        this.questions = new ArrayList<>(size);
        try {
            for (int i = 0; i < size; i++) {
                addQuestion(readQuestion(buf));
            }
        } catch (IndexOutOfBoundsException e) {
            throw new SerializationException(getClass(), "Read question error", e);
        }
    }


    public List<Question> getQuestions() {
        List<Question> questions = this.questions;
        if (questions == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(questions);
    }


    public void addQuestion(Question question) {
        List<Question> questions = this.questions;
        if (questions == null) {
            questions = new ArrayList<>(1);
            this.questions = questions;
        }

        questions.add(Objects.requireNonNull(question));
        questionCount++;
    }
}
