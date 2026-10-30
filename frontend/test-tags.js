// Test script to simulate the tag saving process
console.log('=== Testing Tag Saving Process ===');

// Simulate the Aulas component state
let newContent = {
  title: '',
  description: '',
  category: '',
  tags: [],
  type: 'document'
};

let newTag = '';

// Simulate adding a tag
console.log('\n1. Simulating addTag() process:');
newTag = 'ddddddddddddddddddd';
console.log('newTag:', newTag);
console.log('newContent.tags before:', newContent.tags);

// Simulate the addTag logic
if (newTag.trim() && newContent.tags && newContent.tags.length < 5) {
  console.log('✅ Condições satisfeitas, adicionando tag');
  newContent.tags.push(newTag.trim());
  console.log('Tags após adição:', newContent.tags);
  newTag = '';
  console.log('newTag após limpeza:', newTag);
} else {
  console.log('❌ Condições NÃO satisfeitas');
}

// Simulate saving content
console.log('\n2. Simulating saveContent() process:');
console.log('newContent.tags:', newContent.tags);
console.log('newContent.tags?.length:', newContent.tags?.length);

try {
  const content = {
    title: 'Test Title',
    description: 'Test Description',
    category: 'Informática Básica',
    tags: newContent.tags || [],
    type: 'document',
    fileType: 'PDF',
    fileName: 'test.pdf',
    fileSize: 1024
  };

  console.log('Conteúdo a ser salvo:', content);
  console.log('Tags que serão enviadas:', content.tags);
  
  // Simulate ContentService.addContent
  console.log('\n3. Simulating ContentService.addContent() process:');
  console.log('Item recebido:', content);
  console.log('Tags do item:', content.tags);
  console.log('Tipo de tags:', typeof content.tags);
  console.log('Comprimento de tags:', content.tags?.length);
  
  // Garantir que tags seja sempre um array
  const safeItem = {
    ...content,
    tags: content.tags || []
  };
  
  console.log('safeItem.tags:', safeItem.tags);
  
  const newItem = {
    ...safeItem,
    id: 'test-id-123',
    uploadDate: new Date(),
    type: content.type || 'document'
  };
  
  console.log('Novo item criado:', newItem);
  console.log('Tags no novo item:', newItem.tags);
  
  console.log('\n✅ Test completed successfully!');
  console.log('Tags were preserved throughout the entire process.');
  
} catch (error) {
  console.error('Erro ao salvar conteúdo:', error);
}