package com.aura.livewallpaper.renderer

import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * OpenGL ES 2.0/3.0 için basit bir full-screen quad renderer
 * Fraktal shader'ı ekrana uygular
 */
class FullScreenQuad {
    
    private val vertexData = floatArrayOf(
        // Pozisyon (x, y)   // Tekstür koordinatları (u, v)
        -1f, -1f,           0f, 0f,
         1f, -1f,           1f, 0f,
        -1f,  1f,           0f, 1f,
         1f,  1f,           1f, 1f
    )
    
    private val vertexBuffer: FloatBuffer
    private val indexData = shortArrayOf(0, 1, 2, 2, 1, 3)
    private val indexBuffer: java.nio.ShortBuffer
    
    private var vertexArrayId = 0
    private var vertexAttribLocation = -1
    private var texCoordAttribLocation = -1
    private var programId = 0
    
    init {
        // Vertex buffer oluştur
        val bb = ByteBuffer.allocateDirect(vertexData.size * 4)
        bb.order(ByteOrder.nativeOrder())
        vertexBuffer = bb.asFloatBuffer()
        vertexBuffer.put(vertexData)
        vertexBuffer.position(0)
        
        // Index buffer oluştur
        val ibb = ByteBuffer.allocateDirect(indexData.size * 2)
        ibb.order(ByteOrder.nativeOrder())
        indexBuffer = ibb.asShortBuffer()
        indexBuffer.put(indexData)
        indexBuffer.position(0)
    }
    
    fun createProgram(vertexShader: String, fragmentShader: String): Int {
        // Shader'ları derle
        val vertexShaderId = loadShader(GLES20.GL_VERTEX_SHADER, vertexShader)
        val fragmentShaderId = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShader)
        
        // Program oluştur ve shader'ları bağla
        programId = GLES20.glCreateProgram()
        GLES20.glAttachShader(programId, vertexShaderId)
        GLES20.glAttachShader(programId, fragmentShaderId)
        GLES20.glLinkProgram(programId)
        
        // Derleme durumunu kontrol et
        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(programId, GLES20.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] != GLES20.GL_TRUE) {
            val errorLog = GLES20.glGetProgramInfoLog(programId)
            throw RuntimeException("Program link hatası: $errorLog")
        }
        
        // Attribute location'ları al
        vertexAttribLocation = GLES20.glGetAttribLocation(programId, "aPosition")
        texCoordAttribLocation = GLES20.glGetAttribLocation(programId, "aTexCoord")
        
        return programId
    }
    
    fun useProgram() {
        GLES20.glUseProgram(programId)
    }
    
    fun getUniformLocation(name: String): Int {
        return GLES20.glGetUniformLocation(programId, name)
    }
    
    fun draw() {
        GLES20.glEnableVertexAttribArray(vertexAttribLocation)
        GLES20.glEnableVertexAttribArray(texCoordAttribLocation)
        
        // Vertex buffer'ı bağla (pozisyon + texcoord interleaved)
        val stride = 4 * java.lang.Float.SIZE / 8 // 4 float * 4 bytes
        
        vertexBuffer.position(0)
        GLES20.glVertexAttribPointer(vertexAttribLocation, 2, GLES20.GL_FLOAT, false, stride, vertexBuffer)
        
        vertexBuffer.position(2)
        GLES20.glVertexAttribPointer(texCoordAttribLocation, 2, GLES20.GL_FLOAT, false, stride, vertexBuffer)
        
        // Draw call
        GLES20.glDrawElements(GLES20.GL_TRIANGLE_STRIP, indexData.size, GLES20.GL_UNSIGNED_SHORT, indexBuffer)
        
        GLES20.glDisableVertexAttribArray(vertexAttribLocation)
        GLES20.glDisableVertexAttribArray(texCoordAttribLocation)
    }
    
    fun cleanup() {
        if (programId != 0) {
            GLES20.glDeleteProgram(programId)
            programId = 0
        }
        if (vertexArrayId != 0) {
            GLES20.glDeleteBuffers(1, intArrayOf(vertexArrayId), 0)
            vertexArrayId = 0
        }
    }
    
    private fun loadShader(type: Int, source: String): Int {
        val shaderId = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shaderId, source)
        GLES20.glCompileShader(shaderId)
        
        // Derleme durumunu kontrol et
        val compileStatus = IntArray(1)
        GLES20.glGetShaderiv(shaderId, GLES20.GL_COMPILE_STATUS, compileStatus, 0)
        if (compileStatus[0] != GLES20.GL_TRUE) {
            val errorLog = GLES20.glGetShaderInfoLog(shaderId)
            throw RuntimeException("Shader derleme hatası: $errorLog")
        }
        
        return shaderId
    }
}
