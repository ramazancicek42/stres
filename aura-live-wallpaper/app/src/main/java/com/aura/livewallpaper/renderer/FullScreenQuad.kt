package com.aura.livewallpaper.renderer

import android.opengl.GLES30
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * OpenGL ES 3.0 için full-screen quad renderer
 * Professional fraktal shader'ı ekrana uygular
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
    private var vertexBufferId = 0
    private var indexBufferId = 0
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
        val vertexShaderId = loadShader(GLES30.GL_VERTEX_SHADER, vertexShader)
        val fragmentShaderId = loadShader(GLES30.GL_FRAGMENT_SHADER, fragmentShader)
        
        // Program oluştur ve shader'ları bağla
        programId = GLES30.glCreateProgram()
        GLES30.glAttachShader(programId, vertexShaderId)
        GLES30.glAttachShader(programId, fragmentShaderId)
        GLES30.glLinkProgram(programId)
        
        // Derleme durumunu kontrol et
        val linkStatus = IntArray(1)
        GLES30.glGetProgramiv(programId, GLES30.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] != GLES30.GL_TRUE) {
            val errorLog = GLES30.glGetProgramInfoLog(programId)
            throw RuntimeException("Program link hatası: $errorLog")
        }
        
        // Shader'ları temizle (artık gerek yok)
        GLES30.glDeleteShader(vertexShaderId)
        GLES30.glDeleteShader(fragmentShaderId)
        
        // VAO ve VBO oluştur
        val vaos = IntArray(1)
        GLES30.glGenVertexArrays(1, vaos, 0)
        vertexArrayId = vaos[0]
        
        val vbos = IntArray(2)
        GLES30.glGenBuffers(2, vbos, 0)
        vertexBufferId = vbos[0]
        indexBufferId = vbos[1]
        
        // VAO'yu bağla
        GLES30.glBindVertexArray(vertexArrayId)
        
        // Vertex buffer'ı bağla ve veri yükle
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vertexBufferId)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, vertexData.size * 4, vertexBuffer, GLES30.GL_STATIC_DRAW)
        
        // Index buffer'ı bağla ve veri yükle
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, indexBufferId)
        GLES30.glBufferData(GLES30.GL_ELEMENT_ARRAY_BUFFER, indexData.size * 2, indexBuffer, GLES30.GL_STATIC_DRAW)
        
        // Attribute location'ları al
        vertexAttribLocation = GLES30.glGetAttribLocation(programId, "aPosition")
        texCoordAttribLocation = GLES30.glGetAttribLocation(programId, "aTexCoord")
        
        // Vertex attribute ayarları
        val stride = 4 * 4 // 4 float * 4 bytes
        
        // aPosition (location 0)
        GLES30.glEnableVertexAttribArray(vertexAttribLocation)
        GLES30.glVertexAttribPointer(vertexAttribLocation, 2, GLES30.GL_FLOAT, false, stride, 0)
        
        // aTexCoord (location 1)
        GLES30.glEnableVertexAttribArray(texCoordAttribLocation)
        GLES30.glVertexAttribPointer(texCoordAttribLocation, 2, GLES30.GL_FLOAT, false, stride, 2 * 4)
        
        // VAO'yu bırak
        GLES30.glBindVertexArray(0)
        
        return programId
    }
    
    fun useProgram() {
        GLES30.glUseProgram(programId)
        GLES30.glBindVertexArray(vertexArrayId)
    }
    
    fun getUniformLocation(name: String): Int {
        return GLES30.glGetUniformLocation(programId, name)
    }
    
    fun draw() {
        GLES30.glDrawElements(GLES30.GL_TRIANGLE_STRIP, indexData.size, GLES30.GL_UNSIGNED_SHORT, 0)
    }
    
    fun cleanup() {
        if (programId != 0) {
            GLES30.glDeleteProgram(programId)
            programId = 0
        }
        if (vertexArrayId != 0) {
            GLES30.glDeleteVertexArrays(1, intArrayOf(vertexArrayId), 0)
            vertexArrayId = 0
        }
        if (vertexBufferId != 0) {
            GLES30.glDeleteBuffers(1, intArrayOf(vertexBufferId), 0)
            vertexBufferId = 0
        }
        if (indexBufferId != 0) {
            GLES30.glDeleteBuffers(1, intArrayOf(indexBufferId), 0)
            indexBufferId = 0
        }
    }
    
    private fun loadShader(type: Int, source: String): Int {
        val shaderId = GLES30.glCreateShader(type)
        GLES30.glShaderSource(shaderId, source)
        GLES30.glCompileShader(shaderId)
        
        // Derleme durumunu kontrol et
        val compileStatus = IntArray(1)
        GLES30.glGetShaderiv(shaderId, GLES30.GL_COMPILE_STATUS, compileStatus, 0)
        if (compileStatus[0] != GLES30.GL_TRUE) {
            val errorLog = GLES30.glGetShaderInfoLog(shaderId)
            throw RuntimeException("Shader derleme hatası: $errorLog")
        }
        
        return shaderId
    }
}
