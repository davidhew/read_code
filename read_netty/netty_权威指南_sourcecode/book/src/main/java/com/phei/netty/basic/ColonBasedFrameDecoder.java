/**
 * Alipay.com Inc.
 * Copyright (c) 2004-2016 All Rights Reserved.
 */
package com.phei.netty.basic;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.TooLongFrameException;

import java.util.List;

/**
 * 视":"为粘包标志的decoder
 * @author shouru.hw
 * @version $Id: ColonBasedFrameDecoder.java, v 0.1 2016年2月10日 下午4:35:56 shouru.hw Exp $
 */
public class ColonBasedFrameDecoder extends ByteToMessageDecoder {

    /** Maximum length of a frame we're willing to decode.  */
    private final int maxLength;

    /**
     * @param maxLength -- 能够处理的最长的不带":"的字节数，超过此长度，会抛出异常，同时该部分
     * 内容会被丢弃
     */
    public ColonBasedFrameDecoder(int maxLength) {
        this.maxLength = maxLength;
    }

    /**
     * 默认构造器
     */
    public ColonBasedFrameDecoder() {
        this(1024);
    }

    /** 
     * @see io.netty.handler.codec.ByteToMessageDecoder#decode(io.netty.channel.ChannelHandlerContext, io.netty.buffer.ByteBuf, java.util.List)
     */
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        Object decoded = decode(ctx, in);
        if (decoded != null) {
            out.add(decoded);
        }
    }

    protected Object decode(ChannelHandlerContext ctx, ByteBuf buffer) throws Exception {
        final int eol = findEndOfLine(buffer);

        if (eol >= 0) {
            final ByteBuf frame;
            final int length = eol - buffer.readerIndex();

            if (length > maxLength) {
                buffer.readerIndex(eol + 1);
                fail(ctx, length);
                return null;
            }

            frame = buffer.readSlice(length);
            buffer.skipBytes("*".length());

            return frame.retain();
        } else {
            final int length = buffer.readableBytes();
            if (length > maxLength) {
                int discardedBytes = length;
                buffer.readerIndex(buffer.writerIndex());
                fail(ctx, "over " + discardedBytes);

            }
            return null;
        }

    }

    private void fail(final ChannelHandlerContext ctx, int length) {
        fail(ctx, String.valueOf(length));
    }

    private void fail(final ChannelHandlerContext ctx, String length) {
        ctx.fireExceptionCaught(new TooLongFrameException("frame length (" + length
                                                          + ") exceeds the allowed maximum ("
                                                          + maxLength + ')'));
    }

    /**
     * Returns the index in the buffer of the end of line found.
     * Returns -1 if no end of line was found in the buffer.
     */
    private static int findEndOfLine(final ByteBuf buffer) {
        final int n = buffer.writerIndex();
        for (int i = buffer.readerIndex(); i < n; i++) {
            final byte b = buffer.getByte(i);
            if (b == '*') {
                return i;
            }
        }
        return -1; // Not found.
    }

}
