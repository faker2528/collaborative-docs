import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getDocumentList, createDocument, deleteDocument, getDocument } from '@/api/document'

export const useDocumentStore = defineStore('document', () => {
  const documents = ref([])
  const currentDocument = ref(null)
  const loading = ref(false)

  async function fetchDocuments() {
    loading.value = true
    try {
      const res = await getDocumentList()
      if (res.code === 200) {
        documents.value = res.data || []
      }
    } finally {
      loading.value = false
    }
  }

  async function fetchDocument(id) {
    loading.value = true
    try {
      const res = await getDocument(id)
      if (res.code === 200) {
        currentDocument.value = res.data
      }
      return res
    } finally {
      loading.value = false
    }
  }

  async function addDocument(title) {
    const res = await createDocument({ title, content: '' })
    if (res.code === 200) {
      documents.value.unshift(res.data)
    }
    return res
  }

  async function removeDocument(id) {
    const res = await deleteDocument(id)
    if (res.code === 200) {
      const index = documents.value.findIndex(d => d.id === id)
      if (index > -1) {
        documents.value.splice(index, 1)
      }
    }
    return res
  }

  return {
    documents,
    currentDocument,
    loading,
    fetchDocuments,
    fetchDocument,
    addDocument,
    removeDocument
  }
})
